package org.jetlinks.community.network.mqtt;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttServerOptions;
import lombok.SneakyThrows;
import org.jetlinks.community.network.mqtt.client.MqttClientProperties;
import org.jetlinks.community.network.mqtt.client.MqttClientProvider;
import org.jetlinks.community.network.mqtt.client.VertxMqttClient;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.community.network.mqtt.server.vertx.VertxMqttServerProperties;
import org.jetlinks.community.network.mqtt.server.vertx.VertxMqttServerProvider;
import org.jetlinks.community.network.security.DefaultCertificate;
import org.jetlinks.community.network.security.VertxKeyCertTrustOptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public class VertxMqttSslProviderTest {


    static Vertx vertx = Vertx.vertx();

    static MqttServer mqttServer;

    @Test
    @SneakyThrows
    public void test() {
        VertxMqttServerProvider mqttServerManager = new VertxMqttServerProvider(id -> Mono.empty(), vertx);

        DefaultCertificate serverCert = new DefaultCertificate("server", "test");
        DefaultCertificate clientCert = new DefaultCertificate("client", "test");

        byte[] serverKs = StreamUtils.copyToByteArray(new ClassPathResource("server.p12").getInputStream());

        byte[] clientKs = StreamUtils.copyToByteArray(new ClassPathResource("client.p12").getInputStream());

        byte[] trust = StreamUtils.copyToByteArray(new ClassPathResource("trustStore.p12").getInputStream());

        serverCert.initPfxKey(serverKs, "endPass").initPfxTrust(trust, "rootPass");
        clientCert.initPfxKey(clientKs, "endPass").initPfxTrust(trust, "rootPass");

        VertxMqttServerProperties properties = new VertxMqttServerProperties();
        properties.setId("test");
        properties.setInstance(4);
        properties.setSsl(true);
        properties.setOptions(new MqttServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new VertxKeyCertTrustOptions(serverCert))
                .setTrustOptions(new VertxKeyCertTrustOptions(serverCert))
                .setPort(1888));

        mqttServer = mqttServerManager.createNetwork(properties);

        MqttClientProperties propertiesClient = new MqttClientProperties();
        propertiesClient.setHost("127.0.0.1");
        propertiesClient.setPort(1888);
        propertiesClient.setOptions(new MqttClientOptions()
                .setSsl(true)
                .setKeyCertOptions(new VertxKeyCertTrustOptions(clientCert))
                .setTrustOptions(new VertxKeyCertTrustOptions(clientCert)));

        MqttClientProvider provider = new MqttClientProvider(id -> Mono.empty(), vertx,new MockEnvironment());
        VertxMqttClient client = provider.createNetwork(propertiesClient);
        mqttServer.handleConnection()
                .map(MqttConnection::getClientId)
                .doOnNext(System.out::println)
                .take(1)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

}
