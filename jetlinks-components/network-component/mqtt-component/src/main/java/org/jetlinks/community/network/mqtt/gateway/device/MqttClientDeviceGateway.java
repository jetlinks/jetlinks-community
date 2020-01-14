package org.jetlinks.community.network.mqtt.gateway.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Slf4j
public class MqttClientDeviceGateway implements DeviceGateway {

    @Getter
    private String id;

    private MqttClient mqttClient;

    private DeviceRegistry registry;

    private List<String> topics;

    private String protocol;

    private ProtocolSupports protocolSupport;

    private DecodedClientMessageHandler clientMessageHandler;

    private MessageHandler messageHandler;

    private EmitterProcessor<Message> messageProcessor = EmitterProcessor.create(false);

    private FluxSink<Message> sink = messageProcessor.sink();

    private AtomicBoolean started = new AtomicBoolean();

    private List<Disposable> disposable = new CopyOnWriteArrayList<>();

    public MqttClientDeviceGateway(String id,
                                   MqttClient mqttClient,
                                   DeviceRegistry registry,
                                   ProtocolSupports protocolSupport,
                                   String protocol,
                                   DecodedClientMessageHandler clientMessageHandler,
                                   MessageHandler messageHandler,
                                   List<String> topics) {
        this.id = Objects.requireNonNull(id, "id");
        this.mqttClient = Objects.requireNonNull(mqttClient, "mqttClient");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.protocolSupport = Objects.requireNonNull(protocolSupport, "protocolSupport");
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.clientMessageHandler = Objects.requireNonNull(clientMessageHandler, "clientMessageHandler");
        this.messageHandler = Objects.requireNonNull(messageHandler, "messageHandler");
        this.topics = Objects.requireNonNull(topics, "topics");
    }



    protected Mono<ProtocolSupport> getProtocol() {
        return protocolSupport.getProtocol(protocol);
    }

    private void doStart() {
        if (started.getAndSet(true) || !disposable.isEmpty()) {
            return;
        }

        messageHandler
            .handleGetDeviceState(getId(), idPublisher ->
                Flux.from(idPublisher)
                    .map(id -> new DeviceStateInfo(id, DeviceState.online)));

        disposable.add(messageHandler
            .handleSendToDeviceMessage(getId())
            .filter((msg) -> started.get())
            .flatMap(msg -> {
                if (msg instanceof DeviceMessage) {
                    DeviceMessage deviceMessage = ((DeviceMessage) msg);
                    return registry.getDevice(deviceMessage.getDeviceId())
                        .flatMapMany(device -> device.getProtocol()
                            .flatMapMany(protocol ->
                                protocol.getMessageCodec(getTransport())
                                    .flatMapMany(codec -> codec.encode(new MessageEncodeContext() {
                                        @Override
                                        public Message getMessage() {
                                            return deviceMessage;
                                        }

                                        @Override
                                        public DeviceOperator getDevice() {
                                            return device;
                                        }
                                    }))))
                        .flatMap(message -> mqttClient.publish(((MqttMessage) message)));
                }
                return Mono.empty();
            })
            .onErrorContinue((err, res) -> log.error("处理MQTT消息失败", err))
            .subscribe());

        disposable.add(mqttClient
            .subscribe(topics)
            .filter((msg) -> started.get())
            .flatMap(mqttMessage -> getProtocol()
                .flatMap(codec -> codec.getMessageCodec(getTransport()))
                .flatMapMany(codec -> codec.decode(new MessageDecodeContext() {
                    @Override
                    public EncodedMessage getMessage() {
                        return mqttMessage;
                    }

                    @Override
                    public DeviceOperator getDevice() {
                        throw new UnsupportedOperationException();
                    }
                }))
                .cast(DeviceMessage.class)
                .flatMap(msg -> {
                    if (messageProcessor.hasDownstreams()) {
                        sink.next(msg);
                    }
                    return registry
                        .getDevice(msg.getDeviceId())
                        .flatMap(device -> {
                            Mono<Void> handle = clientMessageHandler.handleMessage(device, msg).then();
                            if (msg instanceof DeviceOfflineMessage) {
                                handle = handle.then(device.offline().then());
                            }
                            if (msg instanceof DeviceOnlineMessage) {
                                handle = handle.then(device.online(getId(), getId()).then());
                            }
                            return handle;
                        });
                }))
            .onErrorContinue((err, res) -> log.error("处理MQTT消息失败", err))
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
