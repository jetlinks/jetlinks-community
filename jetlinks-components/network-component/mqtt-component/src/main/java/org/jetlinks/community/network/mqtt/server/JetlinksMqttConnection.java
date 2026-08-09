/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.network.mqtt.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.server.mqtt.MqttAuth;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.reactor.mqtt.server.MqttSubscription;
import org.jetlinks.reactor.mqtt.server.MqttUnsubscription;
import org.jetlinks.reactor.mqtt.server.ServerConnection;
import org.jetlinks.reactor.mqtt.server.ServerReceivedPublish;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class JetlinksMqttConnection implements MqttConnection {

    private static final VarHandle CLOSED;
    private static final VarHandle ACCEPTED;
    private static final VarHandle LAST_PING_TIME;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            CLOSED = lookup.findVarHandle(JetlinksMqttConnection.class, "closed", boolean.class);
            ACCEPTED = lookup.findVarHandle(JetlinksMqttConnection.class, "accepted", boolean.class);
            LAST_PING_TIME = lookup.findVarHandle(JetlinksMqttConnection.class, "lastPingTime", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final ServerConnection connection;

    private long lastPingTime = System.currentTimeMillis();
    private boolean closed = false;
    private boolean accepted = false;

    @Setter
    private DeviceOperator deviceOperator;
    @Setter
    private AuthenticationResponse authResponse;

    private final Sinks.Many<MqttPublishing> messageProcessor = Reactors.createMany(Integer.MAX_VALUE, false);
    private final Sinks.Many<MqttSubscription> subscription = Reactors.createMany(Integer.MAX_VALUE, false);
    private final Sinks.Many<MqttUnsubscription> unsubscription = Reactors.createMany(Integer.MAX_VALUE, false);

    private Consumer<MqttConnection> disconnectConsumer;

    public JetlinksMqttConnection(ServerConnection connection) {
        this.connection = connection;
        this.disconnectConsumer = conn -> {
            log.debug("mqtt client [{}] disconnected", getClientId());
            subscription.tryEmitComplete();
            unsubscription.tryEmitComplete();
            messageProcessor.tryEmitComplete();
        };

        // 监听连接关闭
        connection.onClose()
                  .doFinally(signal -> complete())
                  .subscribe();
    }

    @Override
    public Duration getKeepAliveTimeout() {
        return connection.getKeepAliveTimeout();
    }

    @Override
    public void onClose(Consumer<MqttConnection> listener) {
        disconnectConsumer = disconnectConsumer.andThen(listener);
    }

    @Override
    public Optional<MqttAuth> getAuth() {
        org.jetlinks.reactor.mqtt.MqttAuth auth = connection.getAuth();
        if (auth == null || !auth.hasAuth()) {
            return Optional.of(new MqttAuth() {
                @Override
                public String getUsername() {
                    return "";
                }

                @Override
                public String getPassword() {
                    return "";
                }
            });
        }
        return Optional.of(new MqttAuth() {
            @Override
            public String getUsername() {
                return auth.getUsername();
            }

            @Override
            public String getPassword() {
                return auth.getPassword();
            }
        });
    }

    @Override
    public void reject(MqttConnectReturnCode code) {
        if ((boolean) CLOSED.getVolatile(this)) {
            return;
        }
        try {
            connection.reject(code).subscribe();
        } catch (Throwable ignore) {
        }
        complete();
    }

    @Override
    public Optional<MqttMessage> getWillMessage() {
        var will = connection.getWill();
        if (will == null || !will.hasWill()) {
            return Optional.empty();
        }
        // retain payload，调用者需要在处理完成后释放
        ByteBuf payload = will.payload();
        if (payload != null) {
            payload.retain();
        }
        return Optional.of(SimpleMqttMessage
                               .builder()
                               .will(true)
                               .payload(payload != null ? payload : Unpooled.EMPTY_BUFFER)
                               .topic(will.topic())
                               .qosLevel(will.qos().value())
                               .retain(will.retain())
                               .build());
    }

    @Override
    public MqttConnection accept() {
        if ((boolean) ACCEPTED.getVolatile(this)) {
            return this;
        }
        log.debug("mqtt client [{}] connected", getClientId());
        ACCEPTED.setVolatile(this, true);
        try {
            connection.accept().subscribe();
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

    @Override
    public long getLastPingTime() {
        return (long) LAST_PING_TIME.getVolatile(this);
    }

    void ping() {
        LAST_PING_TIME.setVolatile(this, System.currentTimeMillis());
    }

    void init() {
        // 处理发布消息
        connection.handlePublishing(publish -> {
                      ping();
                      boolean hasDownstream = messageProcessor.currentSubscriberCount() > 0;
                      if (hasDownstream) {
                          ByteBuf payload = publish.getPayload();
                          if (payload != null) {
                              payload.retain();
                          }
                          ReactorMqttPublishing publishing = new ReactorMqttPublishing(publish, true);
                          messageProcessor.emitNext(publishing, Reactors.emitFailureHandler());
                      }
                  })
                  .handleSubscribe(sub -> {
                      ping();
                      boolean hasDownstream = subscription.currentSubscriberCount() > 0;
                      if (hasDownstream) {
                          subscription.emitNext(sub, Reactors.emitFailureHandler());
                      }
                  })
                  .handleUnsubscribe(unsub -> {
                      ping();
                      boolean hasDownstream = unsubscription.currentSubscriberCount() > 0;
                      if (hasDownstream) {
                          unsubscription.emitNext(unsub, Reactors.emitFailureHandler());
                      }
                  });
    }

    @Override
    public void setKeepAliveTimeout(Duration duration) {
        connection.setKeepAliveTimeout(duration).subscribe();
    }

    @Override
    public InetSocketAddress getClientAddress() {
        return connection.getClientAddress();
    }

    @Override
    public String getClientId() {
        return connection.getClientId();
    }

    @Override
    public Flux<MqttPublishing> handleMessage() {
        return messageProcessor.asFlux();
    }

    @Override
    public Mono<Void> publish(MqttMessage message) {
        ping();
        ByteBuf payload = message.getPayload();
        MqttPublishMessage publishMessage = MqttMessageBuilders
            .publish()
            .topicName(message.getTopic())
            .payload(payload)
            .qos(MqttQoS.valueOf(message.getQosLevel()))
            .retained(message.isRetain())
            .messageId(message.getMessageId())
            .build();

        return connection.publish(publishMessage);
    }

    @Override
    public Flux<MqttSubscription> handleSubscribe() {
        return subscription.asFlux();
    }

    @Override
    public Flux<MqttUnsubscription> handleUnSubscribe() {
        return unsubscription.asFlux();
    }

    @Override
    public InetSocketAddress address() {
        return getClientAddress();
    }

    @Override
    public Mono<Void> sendMessage(EncodedMessage message) {
        if (message instanceof MqttMessage) {
            return publish((MqttMessage) message);
        }
        return Mono.empty();
    }

    @Override
    public Flux<EncodedMessage> receiveMessage() {
        return handleMessage().cast(EncodedMessage.class);
    }

    @Override
    public void disconnect() {
        close().subscribe();
    }

    @Override
    public boolean isAlive() {
        return connection.isAlive();
    }

    @Override
    public Mono<Void> close() {
        if ((boolean) CLOSED.getVolatile(this)) {
            return Mono.empty();
        }
        return connection.close()
                         .doFinally(signal -> complete());
    }

    private void complete() {
        if (CLOSED.compareAndSet(this, false, true)) {
            disconnectConsumer.accept(this);
        }
    }

    // 内部类：MqttPublishing 实现
    static class ReactorMqttPublishing implements MqttPublishing {

        private final ServerReceivedPublish publish;
        private final String clientId;
        private final boolean needRelease;

        ReactorMqttPublishing(ServerReceivedPublish publish, boolean needRelease) {
            this.publish = publish;
            this.clientId = publish.getClientId();
            this.needRelease = needRelease;
        }

        @Override
        public String getTopic() {
            return publish.getTopic();
        }

        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public int getMessageId() {
            return publish.getMessageId();
        }

        @Override
        public int getQosLevel() {
            return publish.getQos().value();
        }

        @Override
        public boolean isDup() {
            return publish.isDup();
        }

        @Override
        public boolean isRetain() {
            return publish.isRetain();
        }

        @Override
        public ByteBuf getPayload() {
            return publish.getPayload();
        }

        @Override
        public String toString() {
            return print();
        }

        @Override
        public MqttProperties getProperties() {
            return publish.getProperties();
        }

        @Override
        public MqttMessage getMessage() {
            return this;
        }

        @Override
        public void acknowledge() {
            if (needRelease) {
                ByteBuf payload = publish.getPayload();
                if (payload != null) {
                    ReferenceCountUtil.safeRelease(payload);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JetlinksMqttConnection that = (JetlinksMqttConnection) o;
        return Objects.equals(connection, that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection);
    }

    @Override
    public Optional<DeviceOperator> getDeviceOperator() {
        return Optional.ofNullable(deviceOperator);
    }

    @Override
    public Optional<AuthenticationResponse> getAuthResponse() {
        return Optional.ofNullable(authResponse);
    }

}
