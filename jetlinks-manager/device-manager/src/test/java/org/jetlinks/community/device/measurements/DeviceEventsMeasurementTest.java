package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.dashboard.MeasurementDimension;
import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.dashboard.MeasurementValue;
import org.jetlinks.community.dashboard.SimpleMeasurementValue;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceEventsMeasurementTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void fromHistory() {
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
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
        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        //DeviceOperator deviceOperator = InMemoryDeviceRegistry.create().register(deviceInstanceEntity.toDeviceInfo()).block();
        DeviceMetadata deviceMetadata = deviceProductOperator.getMetadata().block();

        DeviceEventsMeasurement measurement = new DeviceEventsMeasurement(DEVICE_ID, new BrokerEventBus(), deviceMetadata, dataService);
        measurement.fromHistory(DEVICE_ID, 0).subscribe();

        DeviceEvent deviceEvent = new DeviceEvent();
        Mockito.when(dataService.queryEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(QueryParamEntity.class), Mockito.anyBoolean()))
            .thenReturn(Flux.just(deviceEvent));
        measurement.fromHistory(DEVICE_ID, 1)
            .map(SimpleMeasurementValue::getTimestamp)
            .as(StepVerifier::create)
            .expectNext(0L)
            .verifyComplete();

        measurement.fromRealTime(DEVICE_ID)
            .subscribe();

    }


    //RealTimeDevicePropertyDimension类
    @Test
    void fromRealTime() throws Exception {
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
        DeviceMetadata deviceMetadata = Mockito.mock(DeviceMetadata.class);
        EventBus eventBus = Mockito.mock(EventBus.class);
        DeviceEventsMeasurement measurement = new DeviceEventsMeasurement(DEVICE_ID, eventBus, deviceMetadata, dataService);

        Class<? extends DeviceEventsMeasurement> measurementClass = measurement.getClass();
        Class<?>[] classes = measurementClass.getDeclaredClasses();
        MeasurementDimension dimension = null;
        for (Class<?> aClass : classes) {
            Constructor constructor = (Constructor) aClass.getDeclaredConstructor(measurementClass);
            constructor.setAccessible(true);
            dimension = (MeasurementDimension) constructor.newInstance(measurement);
        }
        assertNotNull(dimension);
        DataType valueType = dimension.getValueType();
        assertNotNull(valueType);

        ConfigMetadata params = dimension.getParams();
        assertNotNull(params);

        boolean realTime = dimension.isRealTime();
        assertTrue(realTime);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("deviceId", DEVICE_ID);
        MeasurementParameter parameter = MeasurementParameter.of(params1);
        Flux<MeasurementValue> value = (Flux<MeasurementValue>) dimension.getValue(parameter);
//      value.map(MeasurementValue::getTimestamp)
//          .as(StepVerifier::create)
//          .expectNext(0L)
//          .verifyComplete();
        EventMessage eventMessage = new EventMessage();
        eventMessage.setEvent("event");
        eventMessage.setData("data");
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class),Mockito.any(Class.class)))
            .thenReturn(Flux.just(eventMessage));
        value.map(MeasurementValue::getValue)
            .map(map->((Map<String, Object>)map).get("data"))
            .as(StepVerifier::create)
            .expectNext("data")
            .verifyComplete();
    }
}