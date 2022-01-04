package org.jetlinks.community.device.message;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceStateInfo;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.core.utils.TopicUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceBatchOperationSubscriptionProviderTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void id() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceBatchOperationSubscriptionProvider provider = new DeviceBatchOperationSubscriptionProvider(instanceService);
        String id = provider.id();
        assertNotNull(id);
        assertEquals("device-batch-operator", id);
    }

    @Test
    void name() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceBatchOperationSubscriptionProvider provider = new DeviceBatchOperationSubscriptionProvider(instanceService);
        String name = provider.name();
        assertNotNull(name);
        assertEquals("设备批量操作", name);
    }

    @Test
    void getTopicPattern() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceBatchOperationSubscriptionProvider provider = new DeviceBatchOperationSubscriptionProvider(instanceService);
        String[] topicPattern = provider.getTopicPattern();
        assertNotNull(topicPattern);
        assertEquals("/device-batch/*", topicPattern[0]);
    }

    @Test
    void subscribe() {
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        DeviceBatchOperationSubscriptionProvider provider = new DeviceBatchOperationSubscriptionProvider(instanceService);
        SubscribeRequest request = new SubscribeRequest();
        request.setId("test");
        request.setTopic("/device-batch/state-sync");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("query", "{\"excludes\":[],\"firstPageIndex\":0,\"forUpdate\":false,\"includes\":[],\"pageIndex\":0,\"pageSize\":25,\"paging\":true,\"parallelPager\":false,\"sorts\":[],\"terms\":[],\"thinkPageIndex\":0}");
        request.setParameter(parameter);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        Mockito.when(instanceService.query(Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.just(deviceInstanceEntity));
        List<DeviceStateInfo> list = new ArrayList<>();
        DeviceStateInfo deviceStateInfo = new DeviceStateInfo();
        deviceStateInfo.setDeviceId(DEVICE_ID);
        deviceStateInfo.setState(DeviceState.online);
        Mockito.when(instanceService.syncStateBatch(Mockito.any(Flux.class), Mockito.anyBoolean()))
            .thenReturn(Flux.just(list));
        assertNotNull(provider);
        provider.subscribe(request)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        SubscribeRequest request1 = new SubscribeRequest();
        request1.setId("test");
        request1.setTopic("/device-batch/deploy");
        Map<String, Object> parameter1 = new HashMap<>();
        String s = "{\"excludes\":[],\"firstPageIndex\":0,\"forUpdate\":false,\"includes\":[],\"pageIndex\":0,\"pageSize\":25,\"paging\":true,\"parallelPager\":false,\"sorts\":[],\"terms\":[],\"thinkPageIndex\":0}";
        Map<String, Object> map = JSON.parseObject(s, Map.class);
        parameter1.put("query", map);
        request1.setParameter(parameter1);
        DeviceDeployResult deviceDeployResult = new DeviceDeployResult();
        deviceDeployResult.setTotal(1);
        Mockito.when(instanceService.deploy(Mockito.any(Flux.class)))
            .thenReturn(Flux.just(deviceDeployResult));
        provider.subscribe(request1)
            .map(a->(DeviceDeployResult)a)
            .map(DeviceDeployResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        request.setTopic("/device-batch/test");
        provider.subscribe(request)
            .as(StepVerifier::create)
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void test() {
        Map<String, String> var = TopicUtils.getPathVariables("/device-batch/{type}", "/device-batch/state-sync");
        String type = var.get("type");
        //System.out.println(type);
        assertNotNull(type);
    }
}