package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.dashboard.MeasurementValue;
import org.jetlinks.community.dashboard.SimpleMeasurementValue;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceEventMeasurementTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void fromHistory() {
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

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
        EventMetadata eventMetadata = deviceProductOperator.getMetadata().map(s -> s.getEvents().get(0)).block();
        EventBus eventBus = Mockito.mock(EventBus.class);
        DeviceEventMeasurement measurement = new DeviceEventMeasurement(PRODUCT_ID, eventBus, eventMetadata, dataService);

        measurement.fromHistory(DEVICE_ID,0).subscribe();

        DeviceEvent deviceEvent = new DeviceEvent();

        Mockito.when(dataService.queryEvent(Mockito.anyString(),Mockito.anyString(),
            Mockito.any(QueryParamEntity.class),Mockito.anyBoolean()))
            .thenReturn(Flux.just(deviceEvent));
        measurement.fromHistory(DEVICE_ID,1)
            .map(SimpleMeasurementValue::getTimestamp)
            .as(StepVerifier::create)
            .expectNext(0L)
            .verifyComplete();

        EventMessage message = new EventMessage();
        message.setData("test");
        message.setTimestamp(System.currentTimeMillis());
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class),Mockito.any(Class.class)))
            .thenReturn(Flux.just(message));
        measurement.fromRealTime(DEVICE_ID)
            .map(MeasurementValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        DeviceEventMeasurement.RealTimeDeviceEventDimension dimension = measurement.new RealTimeDeviceEventDimension();
        DataType valueType = dimension.getValueType();
        assertNotNull(valueType);

        ConfigMetadata params = dimension.getParams();
        assertNotNull(params);

        boolean realTime = dimension.isRealTime();
        assertTrue(realTime);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("deviceId",DEVICE_ID);
        params1.put("history",0);
        MeasurementParameter parameter = new MeasurementParameter();
        parameter.setParams(params1);

        dimension.getValue(parameter).subscribe();
    }

    @Test
    void fromRealTime() {

    }
}