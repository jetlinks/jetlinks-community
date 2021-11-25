package org.jetlinks.community.network.manager.debug;


import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.DeviceGatewayService;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.manager.test.spring.TestJetLinksController;
import org.jetlinks.community.network.manager.test.web.TestAuthentication;
import org.jetlinks.community.network.mqtt.client.VertxMqttClient;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(MqttClientDebugSubscriptionProvider.class)
class MqttClientDebugSubscriptionProviderTest extends TestJetLinksController{

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

    @Resource
    private NetworkProvider<Object> mqttClientProvider;

    @Autowired
    private NetworkConfigService networkConfigService;
    @Autowired
    private DeviceGatewayService deviceGatewayService;

    @Test
    void subscribe() {
//        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        DefaultNetworkManager manager = new DefaultNetworkManager(networkConfigService);
        manager.register(mqttClientProvider);
        MqttClientDebugSubscriptionProvider provider = new MqttClientDebugSubscriptionProvider(manager);
//        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
//            .thenReturn(Mono.just(new VertxMqttClient("TEST")));
//        DeviceGatewayEntity entity = new DeviceGatewayEntity();
//        entity.setId("test");
//        entity.setNetworkId("test");
//        entity.setName("test");
//        entity.setState(NetworkConfigState.enabled);
//        entity.setProvider("tcp-server-gateway");
//        Map<String, Object> map =  new HashMap<>();
//        map.put("protocol", "test");
//        entity.setConfiguration(map);
//        deviceGatewayService.save(entity).subscribe();

        NetworkConfigEntity configEntity = new NetworkConfigEntity();
        configEntity.setId("MQTT_CLIENT");
        Map<String, Object> map1=  new HashMap<>();
        map1.put("parserType", PayloadParserType.DIRECT);
        map1.put("port",1884);
        map1.put("host","127.0.0.1");
        configEntity.setConfiguration(map1);
        configEntity.setState(NetworkConfigState.enabled);
        configEntity.setName("test");
        configEntity.setType("MQTT_CLIENT");
        configEntity.setDescription("test");
        networkConfigService.save(configEntity).subscribe();

        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/mqtt/client/MQTT_CLIENT/_subscribe/STRING");
        Map<String, Object> parameter = new HashMap<>();
        request.setParameter(parameter);
        //provider.subscribe(request).as(StepVerifier::create).expectComplete().verify(Duration.ofSeconds(5));
        provider.subscribe(request).blockFirst(Duration.ofSeconds(5));
//        TestPublisher.create();
    }

    @Test
    void mqttClientSubscribe() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttClientDebugSubscriptionProvider provider = new MqttClientDebugSubscriptionProvider(networkManager);
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