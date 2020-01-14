package org.jetlinks.community.network.mqtt.server.vertx;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.MqttTopicSubscription;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.community.network.mqtt.server.MqttPublishing;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.community.network.mqtt.server.MqttSubscription;
import org.jetlinks.community.network.mqtt.server.MqttUnSubscription;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;


class VertxMqttServerProviderTest {

    static Vertx vertx = Vertx.vertx();

    static MqttServer mqttServer;

    @BeforeAll
    static void init() {
        VertxMqttServerProvider mqttServerManager = new VertxMqttServerProvider(id -> Mono.empty(), vertx);

        VertxMqttServerProperties properties = new VertxMqttServerProperties();
        properties.setId("test");
        properties.setInstance(4);
        properties.setOptions(new MqttServerOptions().setPort(1811));

        mqttServer = mqttServerManager.createNetwork(properties);
    }

    @Test
    void testMessage() {
        MqttClient client = MqttClient.create(vertx);

        client.publishHandler(msg -> {
            System.out.println(msg.topicName() + " " + msg.payload().toString("utf-8"));
        });
        client.connect(1811, "127.0.0.1", result -> {

            if (!result.succeeded()) {
                result.cause().printStackTrace();
            } else {
                client.publish("/test", Buffer.buffer("test"), MqttQoS.AT_MOST_ONCE, false, false);

                client.publish("/test", Buffer.buffer("test1"), MqttQoS.AT_LEAST_ONCE, false, false);

                client.publish("/test", Buffer.buffer("test2"), MqttQoS.EXACTLY_ONCE, false, false);

            }
        });

        mqttServer
                .handleConnection()
                .flatMap(conn -> {
                    Flux<MqttPublishing> publishingFlux = conn.handleMessage();
                    conn.accept();
                    return conn.publish(SimpleMqttMessage.builder()
                            .qosLevel(2)
                            .topic("/test2")
                            .payload(Unpooled.wrappedBuffer("test".getBytes()))
                            .build())
                            .thenMany(publishingFlux);

//                    return publishingFlux;
                })
                .map(pub -> pub.getMessage().getPayload().toString(StandardCharsets.UTF_8))
                .take(3)
                .as(StepVerifier::create)
                .expectNext("test", "test1", "test2")
                .verifyComplete();
    }

    @Test
    void testSub() {
        MqttClient client = MqttClient.create(vertx);
        client.connect(1811, "127.0.0.1", result -> {
            if (!result.succeeded()) {
                result.cause().printStackTrace();
            } else {

                client.subscribe("/test", 1, res -> {
                    System.out.println(res.result());
                });
            }
        });

        mqttServer
                .handleConnection()
                .flatMap(conn -> {
                    Flux<MqttSubscription> publishingFlux = conn.handleSubscribe(true);
                    conn.accept();
                    return publishingFlux;
                })
                .flatMapIterable(pub -> pub.getMessage().topicSubscriptions())
                .map(MqttTopicSubscription::topicName)
                .take(1)
                .as(StepVerifier::create)
                .expectNext("/test")
                .verifyComplete();
    }

    @Test
    void testUnSub() {
        MqttClient client = MqttClient.create(vertx);
        client.connect(1811, "127.0.0.1", result -> {
            if (!result.succeeded()) {
                result.cause().printStackTrace();
            } else {
                client.unsubscribe("/test");
            }
        });

        mqttServer
                .handleConnection()
                .flatMap(conn -> {
                    Flux<MqttUnSubscription> publishingFlux = conn.handleUnSubscribe(true);
                    conn.accept();
                    return publishingFlux;
                })
                .flatMapIterable(pub -> pub.getMessage().topics())
                .take(1)
                .as(StepVerifier::create)
                .expectNext("/test")
                .verifyComplete();
    }


}