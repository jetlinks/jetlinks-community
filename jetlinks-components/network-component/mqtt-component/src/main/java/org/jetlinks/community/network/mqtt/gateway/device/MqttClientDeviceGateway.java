package org.jetlinks.community.network.mqtt.gateway.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttClientSession;
import org.jetlinks.community.network.mqtt.gateway.device.session.UnknownDeviceMqttClientSession;
import org.jetlinks.community.network.utils.DeviceGatewayHelper;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MqttClientDeviceGateway implements DeviceGateway {

    @Getter
    private final String id;

    private final MqttClient mqttClient;

    private final DeviceRegistry registry;

    private final List<String> topics;

    private final String protocol;

    private final ProtocolSupports protocolSupport;

    private final EmitterProcessor<Message> processor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean started = new AtomicBoolean();

    private final List<Disposable> disposable = new CopyOnWriteArrayList<>();

    private final DeviceGatewayMonitor gatewayMonitor;

    private final DeviceGatewayHelper helper;

    public MqttClientDeviceGateway(String id,
                                   MqttClient mqttClient,
                                   DeviceRegistry registry,
                                   ProtocolSupports protocolSupport,
                                   String protocol,
                                   DeviceSessionManager sessionManager,
                                   DecodedClientMessageHandler clientMessageHandler,
                                   List<String> topics) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);

        this.id = Objects.requireNonNull(id, "id");
        this.mqttClient = Objects.requireNonNull(mqttClient, "mqttClient");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.protocolSupport = Objects.requireNonNull(protocolSupport, "protocolSupport");
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.topics = Objects.requireNonNull(topics, "topics");
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }


    protected Mono<ProtocolSupport> getProtocol() {
        return protocolSupport.getProtocol(protocol);
    }

    private void doStart() {
        if (started.getAndSet(true) || !disposable.isEmpty()) {
            return;
        }
        disposable
            .add(mqttClient
                     .subscribe(topics)
                     .filter((msg) -> started.get())
                     .flatMap(mqttMessage -> {
                         AtomicReference<Duration> timeoutRef = new AtomicReference<>();
                         return this
                             .getProtocol()
                             .flatMap(codec -> codec.getMessageCodec(getTransport()))
                             .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(
                                 new UnknownDeviceMqttClientSession(id + ":unknown", mqttClient) {
                                     @Override
                                     public Mono<Boolean> send(EncodedMessage encodedMessage) {
                                         return super
                                             .send(encodedMessage)
                                             .doOnSuccess(r -> gatewayMonitor.sentMessage());
                                     }

                                     @Override
                                     public void setKeepAliveTimeout(Duration timeout) {
                                         timeoutRef.set(timeout);
                                     }
                                 }
                                 , mqttMessage, registry)
                             ))
                             .doOnError((err) -> log.error("解码MQTT客户端消息失败 {}:{}",
                                                           mqttMessage.getTopic(),
                                                           mqttMessage
                                                               .getPayload()
                                                               .toString(StandardCharsets.UTF_8),
                                                           err))
                             .cast(DeviceMessage.class)
                             .flatMap(message -> {
                                 if (processor.hasDownstreams()) {
                                     sink.next(message);
                                 }
                                 gatewayMonitor.receivedMessage();
                                 return helper
                                     .handleDeviceMessage(message,
                                                          device -> createDeviceSession(device, mqttClient),
                                                          DeviceGatewayHelper.applySessionKeepaliveTimeout(message, timeoutRef::get),
                                                          () -> log.warn("无法从MQTT[{}]消息中获取设备信息:{}", mqttMessage.print(), message)
                                     );
                             })
                             .then()
                             .onErrorResume((err) -> {
                                 log.error("处理MQTT消息失败:{}", mqttMessage, err);
                                 return Mono.empty();
                             });
                     }, Integer.MAX_VALUE)
                     .onErrorContinue((err, ms) -> log.error("处理MQTT客户端消息失败", err))
                     .subscribe());
    }

    private MqttClientSession createDeviceSession(DeviceOperator device, MqttClient client) {
        return new MqttClientSession(device.getDeviceId(), device, client, gatewayMonitor);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_CLIENT;
    }

    @Override
    public Flux<Message> onMessage() {
        return processor;
    }

    @Override
    public Mono<Void> pause() {
        return Mono.fromRunnable(() -> started.set(false));
    }

    @Override
    public Mono<Void> startup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            started.set(false);

            disposable.forEach(Disposable::dispose);

            disposable.clear();
        });
    }

    @Override
    public boolean isAlive() {
        return started.get();
    }
}
