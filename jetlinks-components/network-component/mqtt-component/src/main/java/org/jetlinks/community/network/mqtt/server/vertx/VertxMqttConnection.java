package org.jetlinks.community.network.mqtt.server.vertx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
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
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.server.mqtt.MqttAuth;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.*;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
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
    private volatile boolean closed = false, accepted = false, autoAckSub = true, autoAckUnSub = true, autoAckMsg = false;
    private int messageIdCounter;

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
    private final Sinks.Many<MqttPublishing> messageProcessor = Reactors.createMany(Integer.MAX_VALUE, false);
    private final Sinks.Many<MqttSubscription> subscription = Reactors.createMany(Integer.MAX_VALUE, false);
    private final Sinks.Many<MqttUnSubscription> unsubscription = Reactors.createMany(Integer.MAX_VALUE, false);


    public VertxMqttConnection(MqttEndpoint endpoint) {
        this.endpoint = endpoint;
        this.keepAliveTimeoutMs = (endpoint.keepAliveTimeSeconds() + 10) * 1000L;
    }

    private final Consumer<MqttConnection> defaultListener = mqttConnection -> {
        VertxMqttConnection.log.debug("mqtt client [{}] disconnected", getClientId());
        subscription.tryEmitComplete();
        unsubscription.tryEmitComplete();
        messageProcessor.tryEmitComplete();

    };

    private Consumer<MqttConnection> disconnectConsumer = defaultListener;

    @Override
    public Duration getKeepAliveTimeout() {
        return Duration.ofMillis(keepAliveTimeoutMs);
    }

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
        try {
            endpoint.reject(code);
        } catch (Throwable ignore) {
        }
        try {
            complete();
        } catch (Throwable ignore) {

        }
    }

    @Override
    public Optional<MqttMessage> getWillMessage() {
        return Optional.ofNullable(endpoint.will())
                       .filter(will -> will.getWillMessageBytes() != null)
                       .map(will -> SimpleMqttMessage
                           .builder()
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
            .exceptionHandler(error -> {
                if (error instanceof DecoderException) {
                    if (error.getMessage().contains("too large message")) {
                        log.error("MQTT消息过大,请在网络组件中设置[最大消息长度].", error);
                        return;
                    }
                }
                log.error(error.getMessage(), error);
            })
            .pingHandler(ignore -> {
                this.ping();
                if (!endpoint.isAutoKeepAlive()) {
                    endpoint.pong();
                }
            })
            .publishHandler(msg -> {
                ping();
                VertxMqttPublishing publishing = new VertxMqttPublishing(msg, false);
                boolean hasDownstream = this.messageProcessor.currentSubscriberCount() > 0;
                if (autoAckMsg && hasDownstream) {
                    publishing.acknowledge();
                }
                if (hasDownstream) {
                    this.messageProcessor.emitNext(publishing, Reactors.emitFailureHandler());
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
                boolean hasDownstream = this.subscription.currentSubscriberCount() > 0;
                if (autoAckSub || !hasDownstream) {
                    subscription.acknowledge();
                }
                if (hasDownstream) {
                    this.subscription.emitNext(subscription, Reactors.emitFailureHandler());
                }
            })
            .unsubscribeHandler(msg -> {
                ping();
                VertxMqttMqttUnSubscription unSubscription = new VertxMqttMqttUnSubscription(msg, false);
                boolean hasDownstream = this.unsubscription.currentSubscriberCount() > 0;
                if (autoAckUnSub || !hasDownstream) {
                    unSubscription.acknowledge();
                }
                if (hasDownstream) {
                    this.unsubscription.emitNext(unSubscription, Reactors.emitFailureHandler());
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
        try {
            if (clientAddress == null && endpoint != null) {
                SocketAddress address = endpoint.remoteAddress();
                if (address != null) {
                    clientAddress = new InetSocketAddress(address.host(), address.port());
                }
            }
        } catch (Throwable ignore) {

        }
        return clientAddress;
    }

    @Override
    public String getClientId() {
        return endpoint.clientIdentifier();
    }

    @Override
    public Flux<MqttPublishing> handleMessage() {
        return messageProcessor.asFlux();
    }

    @Override
    public Mono<Void> publish(MqttMessage message) {
        ping();
        int messageId = message.getMessageId() <= 0 ? nextMessageId() : message.getMessageId();
        return Mono
            .<Void>create(sink -> {
                ByteBuf buf = message.getPayload();
                Buffer buffer = Buffer.buffer(buf);
                endpoint.publish(
                    message.getTopic(),
                    buffer,
                    MqttQoS.valueOf(message.getQosLevel()),
                    message.isDup(),
                    message.isRetain(),
                    messageId,
                    message.getProperties(),
                    result -> {
                        if (result.succeeded()) {
                            sink.success();
                        } else {
                            sink.error(result.cause());
                        }
                        ReferenceCountUtil.safeRelease(buf);
                    }
                );
            });
    }

    @Override
    public Flux<MqttSubscription> handleSubscribe(boolean autoAck) {

        autoAckSub = autoAck;
        return subscription.asFlux();
    }

    @Override
    public Flux<MqttUnSubscription> handleUnSubscribe(boolean autoAck) {
        autoAckUnSub = autoAck;
        return unsubscription.asFlux();
    }

    @Override
    public InetSocketAddress address() {
        return getClientAddress();
    }

    @Override
    public Mono<Void> sendMessage(EncodedMessage message) {
        if (message instanceof MqttMessage) {
            return publish(((MqttMessage) message));
        }
        return Mono.empty();
    }

    @Override
    public Flux<EncodedMessage> receiveMessage() {
        return handleMessage()
            .cast(EncodedMessage.class);
    }

    @Override
    public void disconnect() {
        close().subscribe();
    }

    @Override
    public boolean isAlive() {
        return endpoint.isConnected() && (keepAliveTimeoutMs < 0 || ((System.currentTimeMillis() - lastPingTime) < keepAliveTimeoutMs));
    }

    @Override
    public Mono<Void> close() {
        if (closed) {
            return Mono.empty();
        }
        return Mono.<Void>fromRunnable(() -> {
            try {
                if (endpoint.isConnected()) {
                    endpoint.close();
                } else {
                    complete();
                }
            } catch (Throwable ignore) {
            }
        });

    }

    private void complete() {
        if (closed) {
            return;
        }
        closed = true;
        disconnectConsumer.accept(this);
    }


    @AllArgsConstructor
    class VertxMqttPublishing implements MqttPublishing {

        private final MqttPublishMessage message;

        private volatile boolean acknowledged;

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

        @Override
        public MqttProperties getProperties() {
            return message.properties();
        }

        @Override
        public MqttMessage getMessage() {
            return this;
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
            endpoint.subscribeAcknowledge(message.messageId(), message
                .topicSubscriptions()
                .stream()
                .map(MqttTopicSubscription::qualityOfService)
                .collect(Collectors.toList()));
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

    private int nextMessageId() {
        this.messageIdCounter = ((this.messageIdCounter % 65535) != 0) ? this.messageIdCounter + 1 : 1;
        return this.messageIdCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VertxMqttConnection that = (VertxMqttConnection) o;
        return Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint);
    }
}
