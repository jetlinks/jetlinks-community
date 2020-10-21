package org.jetlinks.community.network.mqtt.client;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttServer;
import org.jetlinks.core.message.codec.MqttMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


class MqttClientProviderTest {

    private Vertx vertx = Vertx.vertx();

    @Test
    void test() {
        MqttServer server = MqttServer.create(vertx);

        server.endpointHandler(endpoint -> {
            endpoint
                    .accept()
                    .publish("/test", Buffer.buffer("test"), MqttQoS.AT_MOST_ONCE, false, false);
        }).listen(11223);

        MqttClientProvider provider = new MqttClientProvider(id -> Mono.empty(), vertx,new MockEnvironment());

        MqttClientProperties properties = new MqttClientProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(11223);
        properties.setOptions(new MqttClientOptions());

        VertxMqttClient client = provider.createNetwork(properties);

        client.subscribe(Arrays.asList("/test"))
                .map(MqttMessage::getPayload)
                .map(payload -> payload.toString(StandardCharsets.UTF_8))
                .take(1)
                .as(StepVerifier::create)
                .expectNext("test")
                .verifyComplete();


    }

}