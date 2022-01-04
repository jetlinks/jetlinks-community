package org.jetlinks.community.device.message;

import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.StandaloneDeviceMessageBroker;
import org.jetlinks.core.message.ChildDeviceMessage;
import org.jetlinks.core.message.ChildDeviceMessageReply;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.core.server.session.LostDeviceSession;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class DeviceMessageConnectorTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";


    @Test
    void onMessage() {
        DeviceSessionManager sessionManager = Mockito.mock(DeviceSessionManager.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );
        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(deviceOperator);
        LostDeviceSession lostDeviceSession = new LostDeviceSession("test", deviceOperator, DefaultTransport.TCP);

        Mockito.when(sessionManager.onRegister())
            .thenReturn(Flux.just(lostDeviceSession));

        Mockito.when(sessionManager.onUnRegister())
            .thenReturn(Flux.just(lostDeviceSession));

        deviceOperator.setConfig(PropertyConstants.productId.getKey(), PRODUCT_ID).subscribe();
        deviceOperator.setConfig(PropertyConstants.deviceName.getKey(), "test").subscribe();
        deviceOperator.setConfig(PropertyConstants.orgId.getKey(), "org").subscribe();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));
        DeviceMessageConnector connector = new DeviceMessageConnector(new BrokerEventBus(), registry, new StandaloneDeviceMessageBroker(), sessionManager);

        EventMessage eventMessage = new EventMessage();
        eventMessage.setDeviceId(DEVICE_ID);
        ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
        childDeviceMessage.setChildDeviceMessage(eventMessage);
        childDeviceMessage.setDeviceId(DEVICE_ID);
        connector.onMessage(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();


        ChildDeviceMessageReply childDeviceMessageReply = new ChildDeviceMessageReply();
        childDeviceMessageReply.setChildDeviceMessage(eventMessage);
        childDeviceMessageReply.setDeviceId(DEVICE_ID);
        connector.onMessage(childDeviceMessageReply)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        connector.onMessage(null).subscribe();

    }


    @Test
    void handleMessage() {
        DeviceSessionManager sessionManager = Mockito.mock(DeviceSessionManager.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );
        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(deviceOperator);
        LostDeviceSession lostDeviceSession = new LostDeviceSession("test", deviceOperator, DefaultTransport.TCP);

        Mockito.when(sessionManager.onRegister())
            .thenReturn(Flux.just(lostDeviceSession));

        Mockito.when(sessionManager.onUnRegister())
            .thenReturn(Flux.just(lostDeviceSession));

        deviceOperator.setConfig(PropertyConstants.productId.getKey(), PRODUCT_ID).subscribe();
        deviceOperator.setConfig(PropertyConstants.deviceName.getKey(), "test").subscribe();
        deviceOperator.setConfig(PropertyConstants.orgId.getKey(), "org").subscribe();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));
        DeviceMessageConnector connector = new DeviceMessageConnector(new BrokerEventBus(), registry, new StandaloneDeviceMessageBroker(), sessionManager);

        assertNotNull(connector);
        EventMessage eventMessage = new EventMessage();
        eventMessage.setDeviceId(DEVICE_ID);
        ChildDeviceMessageReply childDeviceMessageReply = new ChildDeviceMessageReply();
        childDeviceMessageReply.setChildDeviceMessage(eventMessage);
        childDeviceMessageReply.setDeviceId(DEVICE_ID);
        connector.handleMessage(deviceOperator,childDeviceMessageReply)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        ReadPropertyMessageReply readPropertyMessageReply = new ReadPropertyMessageReply();
        childDeviceMessageReply.setChildDeviceMessage(readPropertyMessageReply);
        connector.handleMessage(deviceOperator,childDeviceMessageReply)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
        childDeviceMessage.setDeviceId(DEVICE_ID);
        childDeviceMessage.setChildDeviceMessage(eventMessage);
        connector.handleMessage(deviceOperator,childDeviceMessage)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        connector.handleMessage(deviceOperator,readPropertyMessageReply)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
        connector.handleMessage(deviceOperator,eventMessage)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void doReply() throws Exception {
        MessageHandler messageHandler = Mockito.mock(MessageHandler.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceSessionManager sessionManager = Mockito.mock(DeviceSessionManager.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );
        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(deviceOperator);

        LostDeviceSession lostDeviceSession = new LostDeviceSession("test", deviceOperator, DefaultTransport.TCP);

        Mockito.when(sessionManager.onRegister())
            .thenReturn(Flux.just(lostDeviceSession));

        Mockito.when(sessionManager.onUnRegister())
            .thenReturn(Flux.just(lostDeviceSession));

        deviceOperator.setConfig(PropertyConstants.productId.getKey(), PRODUCT_ID).subscribe();
        deviceOperator.setConfig(PropertyConstants.deviceName.getKey(), "test").subscribe();
        deviceOperator.setConfig(PropertyConstants.orgId.getKey(), "org").subscribe();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));
        DeviceMessageConnector deviceMessageConnector = new DeviceMessageConnector(new BrokerEventBus(),registry,messageHandler,sessionManager);

        Mockito.when(messageHandler.reply(Mockito.any(DeviceMessageReply.class)))
            .thenReturn(Mono.error(new IllegalArgumentException()));
        Class<? extends DeviceMessageConnector> aClass = deviceMessageConnector.getClass();
        Method reply = aClass.getDeclaredMethod("doReply", DeviceMessageReply.class);
        reply.setAccessible(true);
        ReadPropertyMessageReply messageReply = new ReadPropertyMessageReply();
        messageReply.setMessageId("test");
        Mono<Boolean>  booleanMono= (Mono<Boolean>) reply.invoke(deviceMessageConnector,messageReply);
        assertNotNull(booleanMono);
        booleanMono.as(StepVerifier::create)
            .expectError(IllegalArgumentException.class)
            .verify();

        DeviceMessageConnector.createDeviceMessageTopic(registry,null)
        .as(StepVerifier::create)
        .expectNext("/device/unknown/message/unknown")
        .verifyComplete();
    }
}