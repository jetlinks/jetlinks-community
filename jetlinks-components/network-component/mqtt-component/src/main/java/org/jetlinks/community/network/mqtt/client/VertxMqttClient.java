package org.jetlinks.community.network.mqtt.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.buffer.Buffer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.topic.Topic;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 使用Vertx，MQTT Client。
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class VertxMqttClient implements MqttClient {

    @Getter
    private io.vertx.mqtt.MqttClient client;

    private final Topic<Tuple3<String, FluxSink<MqttMessage>, Integer>> subscriber = Topic.createRoot();

    private final String id;

    private volatile boolean loading;

    private final List<Runnable> loadSuccessListener = new CopyOnWriteArrayList<>();

    //订阅前缀
    @Setter
    private String topicPrefix;

    public void setLoading(boolean loading) {
        this.loading = loading;
        if (!loading) {
            loadSuccessListener.forEach(Runnable::run);
            loadSuccessListener.clear();
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public VertxMqttClient(String id) {
        this.id = id;
    }

    public void setClient(io.vertx.mqtt.MqttClient client) {
        if (this.client != null && this.client != client) {
            try {
                this.client.disconnect();
            } catch (Exception ignore) {

            }
        }
        this.client = client;
        client
            .closeHandler(nil -> log.debug("mqtt client [{}] closed", id))
            .publishHandler(msg -> {
                try {
                    MqttMessage mqttMessage = SimpleMqttMessage
                        .builder()
                        .messageId(msg.messageId())
                        .topic(msg.topicName())
                        .payload(msg.payload().getByteBuf())
                        .dup(msg.isDup())
                        .retain(msg.isRetain())
                        .qosLevel(msg.qosLevel().value())
                        .properties(msg.properties())
                        .build();
                    log.debug("handle mqtt message \n{}", mqttMessage);
                    subscriber
                        .findTopic(msg.topicName().replace("#", "**").replace("+", "*"))
                        .flatMapIterable(Topic::getSubscribers)
                        .subscribe(sink -> {
                            try {
                                sink.getT2().next(mqttMessage);
                            } catch (Exception e) {
                                log.error("handle mqtt message error", e);
                            }
                        });
                } catch (Throwable e) {
                    log.error("handle mqtt message error", e);
                }
            });
        if (loading) {
            loadSuccessListener.add(this::reSubscribe);
        } else if (isAlive()) {
            reSubscribe();
        }

    }

    private void reSubscribe() {
        subscriber
            .getAllSubscriber()
            .filter(topic -> !topic.getSubscribers().isEmpty())
            .collectMap(topic -> getCompleteTopic(convertMqttTopic(topic.getSubscribers().iterator().next().getT1())),
                        topic -> topic.getSubscribers().iterator().next().getT3())
            .filter(MapUtils::isNotEmpty)
            .subscribe(topics -> {
                log.debug("subscribe mqtt topic {}", topics);
                client.subscribe(topics);
            });
    }

    private String convertMqttTopic(String topic) {
        return topic.replace("**", "#").replace("*", "+");
    }

    protected String parseTopic(String topic) {
        //适配emqx共享订阅
        if (topic.startsWith("$share")) {
            topic = Stream.of(topic.split("/"))
                          .skip(2)
                          .collect(Collectors.joining("/", "/", ""));
        } else if (topic.startsWith("$queue")) {
            topic = topic.substring(6);
        }
        if (topic.startsWith("//")) {
            return topic.substring(1);
        }
        return topic;
    }

    //获取完整的topic
    protected String getCompleteTopic(String topic) {
        if (StringUtils.isEmpty(topicPrefix)) {
            return topic;
        }
        return topicPrefix.concat(topic);
    }

    @Override
    public Flux<MqttMessage> subscribe(List<String> topics, int qos) {
        return Flux.create(sink -> {

            Disposable.Composite composite = Disposables.composite();

            for (String topic : topics) {
                String realTopic = parseTopic(topic);
                String completeTopic = getCompleteTopic(topic);

                Topic<Tuple3<String, FluxSink<MqttMessage>, Integer>> sinkTopic = subscriber
                    .append(realTopic
                                .replace("#", "**")
                                .replace("+", "*"));

                Tuple3<String, FluxSink<MqttMessage>, Integer> topicQos = Tuples.of(topic, sink, qos);

                boolean first = sinkTopic.getSubscribers().isEmpty();
                sinkTopic.subscribe(topicQos);
                composite.add(() -> {
                    if (!sinkTopic.unsubscribe(topicQos).isEmpty() && isAlive() && sinkTopic.getSubscribers().isEmpty()) {
                        client.unsubscribe(convertMqttTopic(completeTopic), result -> {
                            if (result.succeeded()) {
                                log.debug("unsubscribe mqtt topic {}", completeTopic);
                            } else {
                                log.debug("unsubscribe mqtt topic {} error", completeTopic, result.cause());
                            }
                        });
                    }
                });

                //首次订阅
                if (isAlive() && first) {
                    log.debug("subscribe mqtt topic {}", completeTopic);
                    client.subscribe(convertMqttTopic(completeTopic), qos, result -> {
                        if (!result.succeeded()) {
                            sink.error(result.cause());
                        }
                    });
                }
            }

            sink.onDispose(composite);

        });
    }

    private Mono<Void> doPublish(MqttMessage message) {
        return Mono.create((sink) -> {
            ByteBuf payload = message.getPayload();
            Buffer buffer = Buffer.buffer(payload);
            client.publish(message.getTopic(),
                           buffer,
                           MqttQoS.valueOf(message.getQosLevel()),
                           message.isDup(),
                           message.isRetain(),
                           result -> {
                               try {
                                   if (result.succeeded()) {
                                       log.info("publish mqtt [{}] message success: {}", client.clientId(), message);
                                       sink.success();
                                   } else {
                                       log.info("publish mqtt [{}] message error : {}", client.clientId(), message, result.cause());
                                       sink.error(result.cause());
                                   }
                               } finally {
                                   ReferenceCountUtil.safeRelease(payload);
                               }
                           });
        });
    }

    @Override
    public Mono<Void> publish(MqttMessage message) {
        if (loading) {
            return Mono.create(sink -> {
                loadSuccessListener.add(() -> {
                    doPublish(message)
                        .doOnSuccess(sink::success)
                        .doOnError(sink::error)
                        .subscribe();
                });
            });
        }
        return doPublish(message);
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
        loading = false;
        if (isAlive()) {
            try {
                client.disconnect();
            } catch (Exception ignore) {

            }
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
