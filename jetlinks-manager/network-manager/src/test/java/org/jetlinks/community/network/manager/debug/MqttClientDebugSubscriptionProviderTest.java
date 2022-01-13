package org.jetlinks.community.network.manager.debug;


import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttServer;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.client.MqttClientProperties;
import org.jetlinks.community.network.mqtt.client.MqttClientProvider;
import org.jetlinks.community.network.mqtt.client.VertxMqttClient;
import org.jetlinks.community.test.web.TestAuthentication;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//@WebFluxTest(MqttClientDebugSubscriptionProvider.class)
class MqttClientDebugSubscriptionProviderTest{
    private Vertx vertx = Vertx.vertx();
    @Test
    void id() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttClientDebugSubscriptionProvider provider = new MqttClientDebugSubscriptionProvider(networkManager);
        String id = provider.id();
        assertNotNull(id);
        assertEquals("network-client-mqtt-debug", id);
    }

    @Test
    void name() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttClientDebugSubscriptionProvider provider = new MqttClientDebugSubscriptionProvider(networkManager);
        String name = provider.name();
        assertNotNull(name);
        assertEquals("MQTT客户端调试", name);
    }

    @Test
    void getTopicPattern() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttClientDebugSubscriptionProvider provider = new MqttClientDebugSubscriptionProvider(networkManager);
        String[] pattern = provider.getTopicPattern();
        assertNotNull(pattern);
    }


    @Test
    void subscribe() {
        assertNotNull(vertx);
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttClientDebugSubscriptionProvider mqttClientDebugSubscriptionProvider = new MqttClientDebugSubscriptionProvider(networkManager);
        MqttServer server = MqttServer.create(vertx);

        server.endpointHandler(endpoint -> {
            endpoint
                .accept()
                .publish("/test", Buffer.buffer("test"), MqttQoS.AT_MOST_ONCE, false, false);
        }).listen(11223);

        MqttClientProvider provider = new MqttClientProvider(id -> Mono.empty(), vertx,new MockEnvironment());

        assertNotNull(provider);
        MqttClientProperties properties = new MqttClientProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(11223);
        properties.setOptions(new MqttClientOptions());

        VertxMqttClient client = provider.createNetwork(properties);
        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
            .thenReturn(Mono.just(client));
        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/mqtt/client/MQTT_CLIENT/_subscribe/STRING");
        Map<String, Object> parameter = new HashMap<>();
        request.setParameter(parameter);
        mqttClientDebugSubscriptionProvider.subscribe(request).subscribe();


        request.setTopic("/network/mqtt/client/MQTT_CLIENT/_publish/STRING");
        mqttClientDebugSubscriptionProvider.subscribe(request);


    }

    @Test
    void mqttClientSubscribe() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttClientDebugSubscriptionProvider provider = new MqttClientDebugSubscriptionProvider(networkManager);
        assertNotNull(provider);
        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/mqtt/client/test/_subscribe/STRING");
        Map<String, Object> parameter = new HashMap<>();
        request.setParameter(parameter);
        provider.mqttClientSubscribe(new VertxMqttClient("TEST"), PayloadType.STRING, request);
    }

    @Test
    void mqttClientPublish() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttClientDebugSubscriptionProvider provider = new MqttClientDebugSubscriptionProvider(networkManager);
        assertNotNull(provider);
        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/mqtt/client/test/_subscribe/STRING");
        Map<String, Object> parameter = new HashMap<>();
        request.setParameter(parameter);
        provider.mqttClientPublish(new VertxMqttClient("TEST"), PayloadType.STRING, request);
    }
}