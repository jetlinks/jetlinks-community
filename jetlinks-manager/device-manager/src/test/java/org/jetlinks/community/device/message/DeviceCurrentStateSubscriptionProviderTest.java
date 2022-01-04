package org.jetlinks.community.device.message;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeviceCurrentStateSubscriptionProviderTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void id() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceCurrentStateSubscriptionProvider provider = new DeviceCurrentStateSubscriptionProvider(instanceService);
        String id = provider.id();
        assertNotNull(id);
        assertEquals("device-state-subscriber", id);
    }

    @Test
    void name() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceCurrentStateSubscriptionProvider provider = new DeviceCurrentStateSubscriptionProvider(instanceService);
        String name = provider.name();
        assertNotNull(name);
        assertEquals("设备当前状态消息", name);
    }

    @Test
    void getTopicPattern() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceCurrentStateSubscriptionProvider provider = new DeviceCurrentStateSubscriptionProvider(instanceService);
        String[] topicPattern = provider.getTopicPattern();
        assertNotNull(topicPattern);
        assertEquals("/device-current-state", topicPattern[0]);
    }

    @Test
    void subscribe() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceCurrentStateSubscriptionProvider provider = new DeviceCurrentStateSubscriptionProvider(instanceService);
        assertNotNull(provider);
        SubscribeRequest request = new SubscribeRequest();
        request.setId("test");
        request.setTopic("/device-batch/state-sync");
        Map<String, Object> parameter = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add(DEVICE_ID);
        parameter.put("deviceId", list);
        request.setParameter(parameter);

        ReactiveQuery<DeviceInstanceEntity> query = Mockito.mock(ReactiveQuery.class);
        Mockito.when(instanceService.createQuery())
            .thenReturn(query);
        Mockito.when(query.select(Mockito.any(StaticMethodReferenceColumn.class)))
            .thenReturn(query);
        Mockito.when(query.in(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Collection.class)))
            .thenReturn(query);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        Mockito.when(query.fetch())
            .thenReturn(Flux.just(deviceInstanceEntity));
        provider.subscribe(request)
            .map(s -> s.get(DEVICE_ID))
            .as(StepVerifier::create)
            .expectNext("online")
            .verifyComplete();

        list.remove(0);
        provider.subscribe(request)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();

        SubscribeRequest request1 = new SubscribeRequest();
//        parameter.put("deviceId", null);
        Executable executable = ()->provider.subscribe(request1);
        assertThrows(IllegalArgumentException.class,executable);



    }
}