package org.jetlinks.community.device.message;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.core.defaults.DefaultDeviceOperator;
import org.jetlinks.core.device.CompositeDeviceMessageSenderInterceptor;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.StandaloneDeviceMessageBroker;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.supports.config.InMemoryConfigStorageManager;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.jetlinks.core.device.DeviceConfigKey.connectionServerId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class DeviceMessageSendSubscriptionProviderTest{
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void id() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceMessageSendSubscriptionProvider provider = new DeviceMessageSendSubscriptionProvider(registry, instanceService);
        String id = provider.id();
        assertNotNull(id);
        assertEquals("device-message-sender", id);
    }

    @Test
    void name() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceMessageSendSubscriptionProvider provider = new DeviceMessageSendSubscriptionProvider(registry, instanceService);
        String name = provider.name();
        assertNotNull(name);
        assertEquals("设备消息发送", name);
    }

    @Test
    void getTopicPattern() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceMessageSendSubscriptionProvider provider = new DeviceMessageSendSubscriptionProvider(registry, instanceService);
        String[] topicPattern = provider.getTopicPattern();
        assertNotNull(topicPattern);
        assertEquals("/device-message-sender/*/*", topicPattern[0]);
    }

    @Test
    void subscribe() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceMessageSendSubscriptionProvider provider = new DeviceMessageSendSubscriptionProvider(registry, instanceService);


        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setTransportProtocol("TCP");
        deviceProductEntity.setProtocolName("演示协议v1");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setCreatorId("1199596756811550720");
        deviceProductEntity.setMessageProtocol("demo-v1");
        deviceProductEntity.setName("TCP测试");
        Map<String, Object> map = new HashMap<>();
        map.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map);
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(connectionServerId.getKey(),"test").subscribe();



        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        SubscribeRequest request = new SubscribeRequest();
        request.setId("test");
        request.setTopic("/device-message-sender/"+PRODUCT_ID+"/"+DEVICE_ID);
        Map<String, Object> parameter = new HashMap<>();

        parameter.put("messageType", MessageType.WRITE_PROPERTY_REPLY);
        request.setParameter(parameter);

        assertNotNull(provider);
        provider.subscribe(request)
            .as(StepVerifier::create)
            .expectError(UnsupportedOperationException.class)
            .verify();
        ReadPropertyMessage readPropertyMessage = new ReadPropertyMessage();
//        System.out.println(readPropertyMessage instanceof MessageType);
        parameter.put("messageType", MessageType.READ_PROPERTY);
        provider.subscribe(request)
            .map(Message::getRequestId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(readPropertyMessageReply));
        DefaultDeviceOperator defaultDeviceOperator =
            new DefaultDeviceOperator(DEVICE_ID, new MockProtocolSupport(), new InMemoryConfigStorageManager()
                , standaloneDeviceMessageBroker, inMemoryDeviceRegistry
                , new CompositeDeviceMessageSenderInterceptor());
        defaultDeviceOperator.setConfig(connectionServerId.getKey(),"test").subscribe();
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(defaultDeviceOperator));
        provider.subscribe(request)
            .map(Message::getRequestId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.error(new IllegalArgumentException()));
        provider.subscribe(request)
            .map(Message::getRequestId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();


        request.setTopic("/device-message-sender/"+PRODUCT_ID+"/*");
        ReactiveQuery<DeviceInstanceEntity> query = Mockito.mock(ReactiveQuery.class);
        Mockito.when(instanceService.createQuery())
            .thenReturn(query);
        Mockito.when(query.select(Mockito.any(StaticMethodReferenceColumn.class)))
            .thenReturn(query);
        Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.fetch())
            .thenReturn(Flux.just(deviceInstanceEntity));

        provider.subscribe(request)
            .map(Message::getRequestId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.empty());
        provider.subscribe(request)
            .map(Message::getRequestId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }
}