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
package org.jetlinks.community.network.mqtt.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.reactor.mqtt.client.ClientConnection;
import org.jetlinks.reactor.mqtt.client.ClientReceivedPublish;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 使用 reactor-mqtt 实现的 MQTT Client。
 *
 * @author PengyuDeng
 * @since 2.11
 */
@Slf4j
public class DefaultJetlinksMqttClient implements JetlinksMqttClient {

    private static final VarHandle CONNECTION;
    private static final VarHandle CONFIG;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            CONNECTION = lookup.findVarHandle(DefaultJetlinksMqttClient.class, "connection", ClientConnection.class);
            CONFIG = lookup.findVarHandle(DefaultJetlinksMqttClient.class, "mqttClientProperties", MqttClientProperties.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused")
    private ClientConnection connection;

    private final String id;

    @Setter
    private String topicPrefix;

    @SuppressWarnings("unused")
    private MqttClientProperties mqttClientProperties;

    public DefaultJetlinksMqttClient(String id) {
        this.id = id;
    }

    public ClientConnection getConnection() {
        return (ClientConnection) CONNECTION.get(this);
    }

    public void setConnection(ClientConnection connection) {
        CONNECTION.set(this, connection);
    }

    public MqttClientProperties getMqttClientProperties() {
        return (MqttClientProperties) CONFIG.get(this);
    }

    public void setMqttClientProperties(MqttClientProperties properties) {
        CONFIG.set(this, properties);
    }

    public boolean isSameConfig(MqttClientProperties properties) {
        MqttClientProperties current = getMqttClientProperties();
        return current != null && current.isSameConfig(properties);
    }

    protected String getCompleteTopic(String topic) {
        if (StringUtils.isEmpty(topicPrefix)) {
            return topic;
        }
        return topicPrefix.concat(topic);
    }

    @Override
    public Flux<MqttMessage> subscribe(List<String> topics, int qos) {
        ClientConnection conn = getConnection();
        if (conn == null || !conn.isAlive()) {
            return Flux.error(new IllegalStateException("MQTT client is not connected"));
        }

        Sinks.Many<MqttMessage> sink = Sinks.many().unicast().onBackpressureBuffer();

        MqttQoS mqttQoS = MqttQoS.valueOf(qos);
        Disposable shareSub;
        if (!StringUtils.isEmpty(topicPrefix)) {
            List<String> completeTopics = topics.stream()
                                                .map(this::getCompleteTopic)
                                                .collect(Collectors.toList());
            shareSub = connection.subscribe(completeTopics, mqttQoS, ignore -> Mono.empty());
        } else {
            shareSub = null;
        }

        Disposable disposable = conn.subscribe(
            topics,
            mqttQoS,
            receivedPublish -> {
                try {
                    MqttMessage mqttMessage = convertToMqttMessage(receivedPublish);
                    log.debug("handle mqtt message \n{}", mqttMessage);
                    Sinks.EmitResult result = sink.tryEmitNext(mqttMessage);
                    if (result.isFailure()) {
                        log.warn("emit mqtt message failed: {}", result);
                    }
                } catch (Exception e) {
                    log.error("handle mqtt message error", e);
                }
                return Mono.empty();
            }
        );

        return sink.asFlux()
                   .doFinally(signal-> {
                       disposable.dispose();
                       if (shareSub != null) {
                           shareSub.dispose();
                       }
                   });
    }

    private MqttMessage convertToMqttMessage(ClientReceivedPublish receivedPublish) {
        ByteBuf payload = receivedPublish.getPayload();
        if (payload != null) {
            payload.retain();
        }
        return SimpleMqttMessage
            .builder()
            .messageId(receivedPublish.getMessageId())
            .topic(receivedPublish.getTopic())
            .payload(payload)
            .dup(receivedPublish.isDup())
            .retain(receivedPublish.isRetain())
            .qosLevel(receivedPublish.getQos().value())
            .properties(receivedPublish.getProperties())
            .build();
    }

    @Override
    public Mono<Void> publish(MqttMessage message) {
        ClientConnection conn = getConnection();
        if (conn == null || !conn.isAlive()) {
            return Mono.error(new IllegalStateException("MQTT client is not connected"));
        }
        return conn
            .publish(
                message.getTopic(),
                message.getPayload(),
                MqttQoS.valueOf(message.getQosLevel()),
                message.isRetain()
            )
            .doOnSuccess(v -> log.info("publish mqtt [{}] message success: {}", id, message))
            .doOnError(e -> log.error("publish mqtt [{}] message error: {}", id, message, e));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_CLIENT;
    }

    @Override
    public void shutdown() {
        shutdownAsync().subscribe();
    }

    /**
     * 异步关闭客户端连接
     *
     * @return 完成信号
     */
    public Mono<Void> shutdownAsync() {
        ClientConnection conn = getConnection();
        if (conn != null && conn.isAlive()) {
            return conn.disconnect()
                       .timeout(Duration.ofSeconds(5))
                       .onErrorResume(e -> {
                           log.warn("mqtt client [{}] disconnect error", id, e);
                           return Mono.empty();
                       });
        }
        return Mono.empty();
    }

    @Override
    public boolean isAlive() {
        ClientConnection conn = getConnection();
        return conn != null && conn.isAlive();
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }
}
