package org.jetlinks.community.network.mqtt.gateway.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttClientSession;
import org.jetlinks.community.network.mqtt.gateway.device.session.UnknownDeviceMqttClientSession;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
public class MqttClientDeviceGateway implements DeviceGateway {

    @Getter
    private final String id;

    private final MqttClient mqttClient;

    private final DeviceRegistry registry;

    private final List<String> topics;

    private final String protocol;

    private final ProtocolSupports protocolSupport;

    private final DecodedClientMessageHandler clientMessageHandler;

    private final EmitterProcessor<Message> messageProcessor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean started = new AtomicBoolean();

    private final List<Disposable> disposable = new CopyOnWriteArrayList<>();

    private final DeviceGatewayMonitor gatewayMonitor;

    private final DeviceSessionManager sessionManager;

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
        this.clientMessageHandler = Objects.requireNonNull(clientMessageHandler, "clientMessageHandler");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        this.topics = Objects.requireNonNull(topics, "topics");
    }


    protected Mono<ProtocolSupport> getProtocol() {
        return protocolSupport.getProtocol(protocol);
    }

    private void doStart() {
        if (started.getAndSet(true) || !disposable.isEmpty()) {
            return;
        }
        disposable.add(mqttClient
            .subscribe(topics)
            .filter((msg) -> started.get())
            .flatMap(mqttMessage -> {
                AtomicReference<Duration> timeoutRef = new AtomicReference<>();
                return getProtocol()
                    .flatMap(codec -> codec.getMessageCodec(getTransport()))
                    .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                        @Nonnull
                        @Override
                        public EncodedMessage getMessage() {
                            return mqttMessage;
                        }

                        @Override
                        public DeviceSession getSession() {
                            return new UnknownDeviceMqttClientSession(id + ":unknown", mqttClient) {
                                @Override
                                public Mono<Boolean> send(EncodedMessage encodedMessage) {
                                    return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
                                }

                                @Override
                                public void setKeepAliveTimeout(Duration timeout) {
                                    timeoutRef.set(timeout);
                                }
                            };
                        }

                        @Override
                        public DeviceOperator getDevice() {
                            return null;
                        }

                        @Override
                        public Mono<DeviceOperator> getDevice(String deviceId) {
                            return registry.getDevice(deviceId);
                        }
                    }))
                    .doOnError((err) -> log.error("解码MQTT客户端消息失败 {}:{}",
                        mqttMessage.getTopic(), mqttMessage.getPayload().toString(StandardCharsets.UTF_8), err))
                    .cast(DeviceMessage.class)
                    .flatMap(msg -> {
                        gatewayMonitor.receivedMessage();
                        if (messageProcessor.hasDownstreams()) {
                            sink.next(msg);
                        }
                        return registry
                            .getDevice(msg.getDeviceId())
                            .switchIfEmpty(Mono.fromRunnable(() -> log.debug("无法识别的设备:{}", msg)))
                            .flatMap(device -> {
                                DeviceSession session = sessionManager.getSession(device.getDeviceId());
                                if (session == null) {
                                    session = new MqttClientSession(id + ":" + device.getDeviceId(), device, mqttClient) {
                                        @Override
                                        public Mono<Boolean> send(EncodedMessage encodedMessage) {
                                            return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
                                        }
                                    };
                                    if (!(msg instanceof DeviceOfflineMessage)) {
                                        sessionManager.register(session);
                                    }
                                }
                                session.keepAlive();
                                if (timeoutRef.get() != null) {
                                    session.setKeepAliveTimeout(timeoutRef.get());
                                }
                                if (msg instanceof DeviceOnlineMessage) {
                                    return Mono.empty();
                                } else if (msg instanceof DeviceOfflineMessage) {
                                    return Mono.fromRunnable(() -> sessionManager.unregister(device.getDeviceId()));
                                } else {
                                    return clientMessageHandler.handleMessage(device, msg).then();
                                }
                            });
                    })
                    .onErrorContinue((err, ms) -> log.error("处理MQTT消息失败:{}", mqttMessage, err));
            })
            .onErrorContinue((err, ms) -> log.error("处理MQTT客户端消息失败", err))
            .subscribe());
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
        return messageProcessor.map(Function.identity());
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
