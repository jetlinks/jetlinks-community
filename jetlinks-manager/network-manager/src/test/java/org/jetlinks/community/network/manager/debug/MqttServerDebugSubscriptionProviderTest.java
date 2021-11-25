package org.jetlinks.community.network.manager.debug;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.impl.MqttTopicSubscriptionImpl;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.manager.test.spring.TestJetLinksController;
import org.jetlinks.community.network.manager.test.web.TestAuthentication;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttPublishing;
import org.jetlinks.community.network.mqtt.server.MqttSubscription;
import org.jetlinks.community.network.mqtt.server.MqttUnSubscription;
import org.jetlinks.community.network.mqtt.server.vertx.VertxMqttServer;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.server.mqtt.MqttAuth;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

//@WebFluxTest(MqttServerDebugSubscriptionProvider.class)
class MqttServerDebugSubscriptionProviderTest  {

    @Test
    void id() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttServerDebugSubscriptionProvider provider = new MqttServerDebugSubscriptionProvider(networkManager);
        String id = provider.id();
        assertNotNull(id);
        assertEquals("network-server-mqtt-debug",id);
    }

    @Test
    void name() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttServerDebugSubscriptionProvider provider = new MqttServerDebugSubscriptionProvider(networkManager);
        String name = provider.name();
        assertNotNull(name);
        assertEquals("MQTT服务调试",name);
    }

    @Test
    void getTopicPattern() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttServerDebugSubscriptionProvider provider = new MqttServerDebugSubscriptionProvider(networkManager);
        String[] pattern = provider.getTopicPattern();
        assertNotNull(pattern);
    }

    @Autowired
    private NetworkConfigService networkConfigService;
    @Test
    void subscribe() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        MqttServerDebugSubscriptionProvider provider = new MqttServerDebugSubscriptionProvider(networkManager);

        VertxMqttServer mqtt_server = new VertxMqttServer("MQTT_SERVER");

        NetworkConfigEntity configEntity = new NetworkConfigEntity();
        configEntity.setId("MQTT_SERVER");
        Map<String, Object> map1=  new HashMap<>();
        map1.put("parserType", PayloadParserType.DIRECT);
        map1.put("port",1884);
        map1.put("host","127.0.0.1");
        configEntity.setConfiguration(map1);
        configEntity.setState(NetworkConfigState.enabled);
        configEntity.setName("test");
        configEntity.setType("MQTT_SERVER");
        configEntity.setDescription("test");
        networkConfigService.save(configEntity).subscribe();

        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
            .thenReturn(Mono.just(mqtt_server));

        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/mqtt/server/MQTT_SERVER/_subscribe/STRING");
        provider.subscribe(request).blockFirst(Duration.ofSeconds(10));
    }

    @Test
    void mqttClientMessage(){
        MqttServerDebugSubscriptionProvider.MqttClientMessage message
            = MqttServerDebugSubscriptionProvider.MqttClientMessage.of("string","test","test");
        String type = message.getType();
        String typeText = message.getTypeText();
        Object data = message.getData();
        assertNotNull(type);
        assertNotNull(typeText);
        assertNotNull(data);

        MqttConnection mqttConnection = Mockito.mock(MqttConnection.class);
        Mockito.when(mqttConnection.getClientId()).thenReturn("clientId");
        Mockito.when(mqttConnection.getClientAddress()).thenReturn(new InetSocketAddress(111));
        MqttAuth mqttAuth = Mockito.mock(MqttAuth.class);
        Mockito.when(mqttConnection.getAuth()).thenReturn(Optional.of(mqttAuth));
        Mockito.when(mqttAuth.getUsername()).thenReturn("a");
        Mockito.when(mqttAuth.getPassword()).thenReturn("b");
        MqttServerDebugSubscriptionProvider.MqttClientMessage message1 = MqttServerDebugSubscriptionProvider.MqttClientMessage.of(mqttConnection);
        assertNotNull(message1);

        MqttServerDebugSubscriptionProvider.MqttClientMessage message2
            = MqttServerDebugSubscriptionProvider.MqttClientMessage.ofDisconnect(mqttConnection);
        assertNotNull(message2);

        MqttSubscription subscription = Mockito.mock(MqttSubscription.class);
        MqttSubscribeMessage mqttSubscribeMessage = Mockito.mock(MqttSubscribeMessage.class);
        Mockito.when(subscription.getMessage())
            .thenReturn(mqttSubscribeMessage);
        MqttTopicSubscriptionImpl mqttTopicSubscription = new MqttTopicSubscriptionImpl("test", MqttQoS.AT_LEAST_ONCE);
        List<MqttTopicSubscription> list = new ArrayList<>();
        list.add(mqttTopicSubscription);
        Mockito.when(mqttSubscribeMessage.topicSubscriptions())
            .thenReturn(list);
        MqttServerDebugSubscriptionProvider.MqttClientMessage message3
            = MqttServerDebugSubscriptionProvider.MqttClientMessage.of(mqttConnection,subscription);
        assertNotNull(message3);

        MqttUnSubscription mqttUnSubscription = Mockito.mock(MqttUnSubscription.class);
        MqttUnsubscribeMessage mqttUnsubscribeMessage = Mockito.mock(MqttUnsubscribeMessage.class);
        Mockito.when(mqttUnSubscription.getMessage())
            .thenReturn(mqttUnsubscribeMessage);
        Mockito.when(mqttUnsubscribeMessage.topics())
            .thenReturn(new ArrayList<>(Collections.singletonList("test")));
        MqttServerDebugSubscriptionProvider.MqttClientMessage message4
            = MqttServerDebugSubscriptionProvider.MqttClientMessage.of(mqttConnection,mqttUnSubscription);
        assertNotNull(message4);


        MqttPublishing mqttPublishing = Mockito.mock(MqttPublishing.class);
        SimpleMqttMessage simpleMqttMessage = new SimpleMqttMessage();
        simpleMqttMessage.setPayload(new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT));
        Mockito.when(mqttPublishing.getMessage())
            .thenReturn(simpleMqttMessage);
        MqttServerDebugSubscriptionProvider.MqttClientMessage message5
            = MqttServerDebugSubscriptionProvider.MqttClientMessage.of(mqttConnection,mqttPublishing, PayloadType.STRING);
        assertNotNull(message5);

    }
}