package org.jetlinks.community.network.mqtt.client;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.core.utils.TopicUtils;
import reactor.core.publisher.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class VertxMqttClient implements MqttClient {

    @Getter
    private io.vertx.mqtt.MqttClient client;

    private final FluxProcessor<MqttMessage, MqttMessage> messageProcessor = EmitterProcessor.create(false);

    private final FluxSink<MqttMessage> sink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final Map<String, AtomicInteger> topicsSubscribeCounter = new ConcurrentHashMap<>();

    private boolean neverSubscribe = true;

    private final String id;

    @Getter
    private final AtomicInteger reloadCounter = new AtomicInteger();

    public VertxMqttClient(String id) {
        this.id = id;
    }

    public void setClient(io.vertx.mqtt.MqttClient client) {
        this.client = client;
        if (isAlive()) {
            reloadCounter.set(0);
            client.publishHandler(msg -> {
                //从未订阅,可能消息是还没来得及
                //或者已经有了下游消费者
                if (neverSubscribe || messageProcessor.hasDownstreams()) {
                    sink.next(SimpleMqttMessage
                        .builder()
                        .topic(msg.topicName())
                        .clientId(client.clientId())
                        .qosLevel(msg.qosLevel().value())
                        .retain(msg.isRetain())
                        .dup(msg.isDup())
                        .payload(msg.payload().getByteBuf())
                        .messageId(msg.messageId())
                        .build());
                }
            });
            if (!topicsSubscribeCounter.isEmpty()) {
                Map<String, Integer> reSubscribe = topicsSubscribeCounter
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().get() > 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toMap(Function.identity(), (r) -> 0));
                if (!reSubscribe.isEmpty()) {
                    log.info("re subscribe [{}] topic {}", client.clientId(), reSubscribe.keySet());
                    client.subscribe(reSubscribe);
                }
            }
        }

    }

    private AtomicInteger getTopicCounter(String topic) {
        return topicsSubscribeCounter.computeIfAbsent(topic, (ignore) -> new AtomicInteger());
    }


    @Override
    public Flux<MqttMessage> subscribe(List<String> topics) {
        neverSubscribe = false;
        AtomicBoolean canceled = new AtomicBoolean();
        return Flux.defer(() -> {
            Map<String, Integer> subscribeTopic = topics.stream()
                .filter(r -> getTopicCounter(r).getAndIncrement() == 0)
                .collect(Collectors.toMap(Function.identity(), (r) -> 0));
            if (isAlive()) {
                if (!subscribeTopic.isEmpty()) {
                    log.info("subscribe mqtt [{}] topic : {}", client.clientId(), subscribeTopic);
                    client.subscribe(subscribeTopic);
                }
            }
            return messageProcessor
                .filter(msg -> topics
                    .stream()
                    .anyMatch(topic -> TopicUtils.match(topic, msg.getTopic())));
        }).doOnCancel(() -> {
            if (!canceled.getAndSet(true)) {
                for (String topic : topics) {
                    if (getTopicCounter(topic).decrementAndGet() <= 0 && isAlive()) {
                        log.info("unsubscribe mqtt [{}] topic : {}", client.clientId(), topic);
                        client.unsubscribe(topic);
                    }
                }
            }
        });
    }

    @Override
    public Mono<Void> publish(MqttMessage message) {
        return Mono.create((sink) -> {
            if (!isAlive()) {
                sink.error(new IOException("mqtt client not alive"));
                return;
            }
            Buffer buffer = Buffer.buffer(message.getPayload());
            client.publish(message.getTopic(),
                buffer,
                MqttQoS.valueOf(message.getQosLevel()),
                message.isDup(),
                message.isRetain(),
                result -> {
                    if (result.succeeded()) {
                        log.info("publish mqtt [{}] message success: {}", client.clientId(), message);
                        sink.success();
                    } else {
                        log.info("publish mqtt [{}] message error : {}", client.clientId(), message, result.cause());
                        sink.error(result.cause());
                    }
                });
        });
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
        if (isAlive()) {
            client.disconnect();
            client = null;
        }

    }

    @Override
    public boolean isAlive() {
        return client != null && client.isConnected();
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

}
