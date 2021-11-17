package org.jetlinks.community.device.message;

import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.PropertyMetadataConstants;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeviceMessageSendLogInterceptorTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void doPublish() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceMessageSendLogInterceptor interceptor = new DeviceMessageSendLogInterceptor(new BrokerEventBus(), registry);
        ReadPropertyMessageReply readPropertyMessageReply = new ReadPropertyMessageReply();
        readPropertyMessageReply.addHeader(Headers.dispatchToParent, true);
        interceptor.doPublish(readPropertyMessageReply).subscribe();
        readPropertyMessageReply.addHeader(Headers.dispatchToParent, false);
        readPropertyMessageReply.setDeviceId(null);
        interceptor.doPublish(readPropertyMessageReply)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();


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
        deviceOperator.setConfig(PropertyConstants.productId.getKey(), PRODUCT_ID).subscribe();
        deviceOperator.setConfig(PropertyConstants.deviceName.getKey(), "TCP-setvice").subscribe();
        deviceOperator.setConfig(PropertyConstants.orgId.getKey(), "test");
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        readPropertyMessageReply.setDeviceId(DEVICE_ID);
        interceptor.doPublish(readPropertyMessageReply)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        ThingPropertyMessage thingPropertyMessage = new ThingPropertyMessage();
        interceptor.doPublish(thingPropertyMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
        ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
        childDeviceMessage.setDeviceId(DEVICE_ID);
        childDeviceMessage.setChildDeviceMessage(thingPropertyMessage);
        interceptor.doPublish(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

    }

    @Test
    void afterSent() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceMessageSendLogInterceptor interceptor = new DeviceMessageSendLogInterceptor(new BrokerEventBus(), registry);

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

        WritePropertyMessage writePropertyMessage = new WritePropertyMessage();
        writePropertyMessage.addHeader(PropertyMetadataConstants.Source.headerKey, "manual");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("test", "test");
        writePropertyMessage.setProperties(properties);
        writePropertyMessage.addHeader(Headers.dispatchToParent, true);
        WritePropertyMessageReply writePropertyMessageReply = new WritePropertyMessageReply();
        interceptor.afterSent(deviceOperator, writePropertyMessage, Flux.just(writePropertyMessageReply))
            .map(WritePropertyMessageReply::getMessageType)
            .as(StepVerifier::create)
            .expectNext(MessageType.WRITE_PROPERTY_REPLY)
            .verifyComplete();

        writePropertyMessage.addHeader(PropertyMetadataConstants.Source.headerKey, "");
        interceptor.afterSent(deviceOperator, writePropertyMessage, Flux.just(writePropertyMessageReply))
            .map(WritePropertyMessageReply::getMessageType)
            .as(StepVerifier::create)
            .expectNext(MessageType.WRITE_PROPERTY_REPLY)
            .verifyComplete();
    }

    @Test
    void preSend() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceMessageSendLogInterceptor interceptor = new DeviceMessageSendLogInterceptor(new BrokerEventBus(), registry);
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
        Map<String, Object> map1 = new HashMap<>();
        map1.put("source","manual");
        deviceOperator.getMetadata().map(s->{
            s.setExpands(map1);
            return Mono.just(1);
        }).subscribe();

        //deviceOperator.getMetadata().map(s->s.getExpand("source")).subscribe(System.out::println);



        WritePropertyMessage writePropertyMessage = new WritePropertyMessage();
        writePropertyMessage.addHeader(PropertyMetadataConstants.Source.headerKey, "manual");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("temperature", "temperature");
        writePropertyMessage.setProperties(properties);
        writePropertyMessage.setDeviceId(DEVICE_ID);
        writePropertyMessage.addHeader(Headers.dispatchToParent, true);
        interceptor.preSend(deviceOperator, writePropertyMessage)
            .map(DeviceMessage::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();

        UpdateTagMessage updateTagMessage = new UpdateTagMessage();
        updateTagMessage.setDeviceId(DEVICE_ID);
        interceptor.preSend(deviceOperator, updateTagMessage)
            .map(DeviceMessage::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();


        ReadPropertyMessage readPropertyMessage = new ReadPropertyMessage();
        readPropertyMessage.setDeviceId(DEVICE_ID);
        readPropertyMessage.addHeader(Headers.dispatchToParent, true);
        interceptor.preSend(deviceOperator, readPropertyMessage)
            .map(DeviceMessage::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();


        FunctionInvokeMessage functionInvokeMessage = new FunctionInvokeMessage();
        functionInvokeMessage.setDeviceId(DEVICE_ID);
        functionInvokeMessage.addHeader(Headers.force, true);
        functionInvokeMessage.addHeader(Headers.dispatchToParent, true);
        interceptor.preSend(deviceOperator, functionInvokeMessage)
            .map(DeviceMessage::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();

        functionInvokeMessage.setFunctionId("cc");
        functionInvokeMessage.addHeader(Headers.force, false);
        interceptor.preSend(deviceOperator, functionInvokeMessage)
            .as(StepVerifier::create)
            .expectError(DeviceOperationException.class)
            .verify();

        functionInvokeMessage.setFunctionId("AuditCommandFunction");
        functionInvokeMessage.addHeader(Headers.force, false);
        List<FunctionParameter> list = new ArrayList<>();
        FunctionParameter functionParameter = new FunctionParameter();
        functionParameter.setName("outTime");
        functionParameter.setValue("12");
        list.add(functionParameter);
        functionInvokeMessage.setInputs(list);
        deviceOperator.updateMetadata("{\"events\":[],\"properties\":[],\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}],\"tags\":[]}").subscribe();

        interceptor.preSend(deviceOperator, functionInvokeMessage)
            .map(DeviceMessage::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();

        FunctionParameter functionParameter1 = new FunctionParameter();
        functionParameter1.setName("test");
        functionParameter1.setValue("12");
        List<FunctionParameter> list1 = new ArrayList<>();
        list1.add(functionParameter1);
        functionInvokeMessage.setInputs(list1);
        interceptor.preSend(deviceOperator, functionInvokeMessage)
            .map(DeviceMessage::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();
    }
}