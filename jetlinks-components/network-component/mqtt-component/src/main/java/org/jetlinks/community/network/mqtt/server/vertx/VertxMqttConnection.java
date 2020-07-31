package org.jetlinks.community.network.mqtt.server.vertx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttPublishing;
import org.jetlinks.community.network.mqtt.server.MqttSubscription;
import org.jetlinks.community.network.mqtt.server.MqttUnSubscription;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.server.mqtt.MqttAuth;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class VertxMqttConnection implements MqttConnection {

    private final MqttEndpoint endpoint;
    private long keepAliveTimeoutMs;
    @Getter
    private long lastPingTime = System.currentTimeMillis();
    private volatile boolean closed = false, accepted = false, autoAckSub = true, autoAckUnSub = true, autoAckMsg = true;

    private final EmitterProcessor<MqttPublishing> messageProcessor = EmitterProcessor.create(false);

    private final FluxSink<MqttPublishing> publishingFluxSink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final EmitterProcessor<MqttSubscription> subscription = EmitterProcessor.create(false);
    private final EmitterProcessor<MqttUnSubscription> unsubscription = EmitterProcessor.create(false);

    private static final MqttAuth emptyAuth = new MqttAuth() {
        @Override
        public String getUsername() {
            return "";
        }

        @Override
        public String getPassword() {
            return "";
        }
    };

    public VertxMqttConnection(MqttEndpoint endpoint) {
        this.endpoint = endpoint;
        this.keepAliveTimeoutMs = (endpoint.keepAliveTimeSeconds() + 10) * 1000L;
    }

    private final Consumer<MqttConnection> defaultListener = mqttConnection -> {
        log.debug("mqtt client [{}] disconnected", getClientId());
        subscription.onComplete();
        unsubscription.onComplete();
        messageProcessor.onComplete();

    };

    private Consumer<MqttConnection> disconnectConsumer = defaultListener;

    @Override
    public void onClose(Consumer<MqttConnection> listener) {
        disconnectConsumer = disconnectConsumer.andThen(listener);
    }

    @Override
    public Optional<MqttAuth> getAuth() {
        return endpoint.auth() == null ? Optional.of(emptyAuth) : Optional.of(new VertxMqttAuth());
    }

    @Override
    public void reject(MqttConnectReturnCode code) {
        if (closed) {
            return;
        }
        endpoint.reject(code);
        complete();
    }

    @Override
    public Optional<MqttMessage> getWillMessage() {
        return Optional.ofNullable(endpoint.will())
            .filter(will -> will.getWillMessageBytes() != null)
            .map(will -> SimpleMqttMessage.builder()
                .will(true)
                .payload(Unpooled.wrappedBuffer(will.getWillMessageBytes()))
                .topic(will.getWillTopic())
                .qosLevel(will.getWillQos())
                .build());
    }

    @Override
    public MqttConnection accept() {
        if (accepted) {
            return this;
        }
        log.debug("mqtt client [{}] connected", getClientId());
        accepted = true;
        try {
            if (!endpoint.isConnected()) {
                endpoint.accept();
            }
        } catch (Exception e) {
            close().subscribe();
            log.warn(e.getMessage(), e);
            return this;
        }
        init();
        return this;
    }

    @Override
    public void keepAlive() {
        ping();
    }

    void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    void init() {
        this.endpoint
            .disconnectHandler(ignore -> this.complete())
            .closeHandler(ignore -> this.complete())
            .pingHandler(ignore -> {
                this.ping();
                if (!endpoint.isAutoKeepAlive()) {
                    endpoint.pong();
                }
            })
            .publishHandler(msg -> {
                ping();
                VertxMqttPublishing publishing = new VertxMqttPublishing(msg, false);
                boolean hasDownstream = this.messageProcessor.hasDownstreams();
                if (autoAckMsg || !hasDownstream) {
                    publishing.acknowledge();
                }
                if (hasDownstream) {
                    this.publishingFluxSink.next(publishing);
                }
            })
            //QoS 1 PUBACK
            .publishAcknowledgeHandler(messageId -> {
                ping();
                log.debug("PUBACK mqtt[{}] message[{}]", getClientId(), messageId);
            })
            //QoS 2  PUBREC
            .publishReceivedHandler(messageId -> {
                ping();
                log.debug("PUBREC mqtt[{}] message[{}]", getClientId(), messageId);
                endpoint.publishRelease(messageId);
            })
            //QoS 2  PUBREL
            .publishReleaseHandler(messageId -> {
                ping();
                log.debug("PUBREL mqtt[{}] message[{}]", getClientId(), messageId);
                endpoint.publishComplete(messageId);
            })
            //QoS 2  PUBCOMP
            .publishCompletionHandler(messageId -> {
                ping();
                log.debug("PUBCOMP mqtt[{}] message[{}]", getClientId(), messageId);
            })
            .subscribeHandler(msg -> {
                ping();
                VertxMqttSubscription subscription = new VertxMqttSubscription(msg, false);
                boolean hasDownstream = this.subscription.hasDownstreams();
                if (autoAckSub || !hasDownstream) {
                    subscription.acknowledge();
                }
                if (hasDownstream) {
                    this.subscription.onNext(subscription);
                }
            })
            .unsubscribeHandler(msg -> {
                ping();
                VertxMqttMqttUnSubscription unSubscription = new VertxMqttMqttUnSubscription(msg, false);
                boolean hasDownstream = this.unsubscription.hasDownstreams();
                if (autoAckUnSub || !hasDownstream) {
                    unSubscription.acknowledge();
                }
                if (hasDownstream) {
                    this.unsubscription.onNext(unSubscription);
                }
            });
    }

    @Override
    public void setKeepAliveTimeout(Duration duration) {
        keepAliveTimeoutMs = duration.toMillis();
    }

    private volatile InetSocketAddress clientAddress;

    @Override
    public InetSocketAddress getClientAddress() {
        if (clientAddress == null) {
            SocketAddress address = endpoint.remoteAddress();
            if (address != null) {
                clientAddress = new InetSocketAddress(address.host(), address.port());
            }
        }
        return clientAddress;
    }

    @Override
    public String getClientId() {
        return endpoint.clientIdentifier();
    }

    @Override
    public Flux<MqttPublishing> handleMessage() {
        if (messageProcessor.isCancelled()) {
            return Flux.empty();
        }
        return messageProcessor
            .map(Function.identity());
    }

    @Override
    public Mono<Void> publish(MqttMessage message) {
        ping();
        return Mono
            .<Void>create(sink -> {
                Buffer buffer = Buffer.buffer(message.getPayload());
                endpoint.publish(message.getTopic(),
                    buffer,
                    MqttQoS.valueOf(message.getQosLevel()),
                    message.isDup(),
                    message.isRetain(),
                    result -> {
                        if (result.succeeded()) {
                            sink.success();
                        } else {
                            sink.error(result.cause());
                        }
                    }
                );
            });
    }

    @Override
    public Flux<MqttSubscription> handleSubscribe(boolean autoAck) {

        autoAckSub = autoAck;
        return subscription.map(Function.identity());
    }

    @Override
    public Flux<MqttUnSubscription> handleUnSubscribe(boolean autoAck) {
        autoAckUnSub = autoAck;
        return unsubscription.map(Function.identity());
    }

    @Override
    public boolean isAlive() {
        return endpoint.isConnected() && (keepAliveTimeoutMs < 0 || ((System.currentTimeMillis() - lastPingTime) < keepAliveTimeoutMs));
    }

    @Override
    public Mono<Void> close() {
        return Mono.<Void>fromRunnable(() -> {
            if (endpoint.isConnected()) {
                endpoint.close();
            }
        }).doFinally(s -> this.complete());

    }

    private void complete() {
        if (closed) {
            return;
        }
        closed = true;
        disconnectConsumer.accept(this);
        disconnectConsumer = defaultListener;
    }

    @AllArgsConstructor
    class VertxMqttMessage implements MqttMessage {
        MqttPublishMessage message;

        @Nonnull
        @Override
        public String getTopic() {
            return message.topicName();
        }

        @Override
        public String getClientId() {
            return VertxMqttConnection.this.getClientId();
        }

        @Override
        public int getMessageId() {
            return message.messageId();
        }

        @Override
        public boolean isWill() {
            return false;
        }

        @Override
        public int getQosLevel() {
            return message.qosLevel().value();
        }

        @Override
        public boolean isDup() {
            return message.isDup();
        }

        @Override
        public boolean isRetain() {
            return message.isRetain();
        }

        @Nonnull
        @Override
        public ByteBuf getPayload() {
            return message.payload().getByteBuf();
        }

        @Override
        public String toString() {
            return print();
        }
    }

    @AllArgsConstructor
    class VertxMqttPublishing implements MqttPublishing {

        private final MqttPublishMessage message;

        private volatile boolean acknowledged;

        @Override
        public MqttMessage getMessage() {
            return new VertxMqttMessage(message);
        }

        @Override
        public void acknowledge() {
            if (acknowledged) {
                return;
            }
            acknowledged = true;
            if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                log.debug("PUBACK QoS1 mqtt[{}] message[{}]", getClientId(), message.messageId());
                endpoint.publishAcknowledge(message.messageId());
            } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                log.debug("PUBREC QoS2 mqtt[{}] message[{}]", getClientId(), message.messageId());
                endpoint.publishReceived(message.messageId());
            }
        }
    }

    @AllArgsConstructor
    class VertxMqttSubscription implements MqttSubscription {

        private final MqttSubscribeMessage message;

        private volatile boolean acknowledged;

        @Override
        public MqttSubscribeMessage getMessage() {
            return message;
        }

        @Override
        public synchronized void acknowledge() {
            if (acknowledged) {
                return;
            }
            acknowledged = true;
            endpoint.subscribeAcknowledge(message.messageId(), message.topicSubscriptions().stream()
                .map(MqttTopicSubscription::qualityOfService).collect(Collectors.toList()));
        }
    }

    @AllArgsConstructor
    class VertxMqttMqttUnSubscription implements MqttUnSubscription {

        private final MqttUnsubscribeMessage message;

        private volatile boolean acknowledged;

        @Override
        public MqttUnsubscribeMessage getMessage() {
            return message;
        }

        @Override
        public synchronized void acknowledge() {
            if (acknowledged) {
                return;
            }
            log.info("acknowledge mqtt [{}] unsubscribe : {} ", getClientId(), message.topics());
            acknowledged = true;
            endpoint.unsubscribeAcknowledge(message.messageId());
        }
    }


    class VertxMqttAuth implements MqttAuth {

        @Override
        public String getUsername() {
            return endpoint.auth().getUsername();
        }

        @Override
        public String getPassword() {
            return endpoint.auth().getPassword();
        }
    }
}
