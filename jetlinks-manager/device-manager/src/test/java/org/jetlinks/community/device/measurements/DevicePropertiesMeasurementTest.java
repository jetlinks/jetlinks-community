package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.dashboard.MeasurementDimension;
import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.dashboard.MeasurementValue;
import org.jetlinks.community.dashboard.SimpleMeasurementValue;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DevicePropertiesMeasurementTest {
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
        DevicePropertiesMeasurement measurement = new DevicePropertiesMeasurement(PRODUCT_ID, new BrokerEventBus(), dataService, deviceMetadata);

        measurement.fromHistory(DEVICE_ID,0,new HashSet<>()).subscribe();
        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        Mockito.when(dataService.queryEachProperties(Mockito.anyString(),Mockito.any(QueryParamEntity.class),Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));
        Set<String> set = new HashSet<>();
        set.add("test");
        measurement.fromHistory(DEVICE_ID,1,set)
            .map(SimpleMeasurementValue::getValue)
            .map(s->((DeviceProperty)s).getValue())
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        Map<String, Object> temperature = measurement.createValue("temperature", 36);
        assertNotNull(temperature);

        Map<String, Object> temperature1 = measurement.createValue("temperature1", 36);
        assertNotNull(temperature1);

        measurement.fromRealTime(DEVICE_ID,new HashSet<>()).subscribe();

        EventBus eventBus = Mockito.mock(EventBus.class);
        DevicePropertiesMeasurement measurement1 = new DevicePropertiesMeasurement(PRODUCT_ID, eventBus, dataService, deviceMetadata);

        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature","temperature");
        reply.setProperties(properties);
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class),Mockito.any(Class.class)))
            .thenReturn(Flux.just(reply));
        measurement1.fromRealTime(DEVICE_ID,new HashSet<>()).subscribe();
    }


    @Test
    void fromRealTime() {
    }

    @Test
    void getPropertiesFromParameter() {
        Map<String, Object> params = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add("test");
        params.put("properties",list);
        MeasurementParameter parameter = MeasurementParameter.of(params);
        Set<String> set = DevicePropertiesMeasurement.getPropertiesFromParameter(parameter);
        assertNotNull(set);
    }

    @Test
    void history() throws Exception {
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
        DeviceMetadata deviceMetadata = Mockito.mock(DeviceMetadata.class);
        DevicePropertiesMeasurement measurement = new DevicePropertiesMeasurement(PRODUCT_ID, new BrokerEventBus(), dataService, deviceMetadata);

        Class<? extends DevicePropertiesMeasurement> measurementClass = measurement.getClass();
        Class<?>[] classes = measurementClass.getDeclaredClasses();
        MeasurementDimension dimension = null;
        for (int i = 0; i < classes.length; i++) {
            if(i==0){
                Constructor constructor = (Constructor) classes[i].getDeclaredConstructor(measurementClass);
                constructor.setAccessible(true);
                dimension = (MeasurementDimension) constructor.newInstance(measurement);

                assertNotNull(dimension);
                DataType valueType = dimension.getValueType();
                assertNotNull(valueType);

                ConfigMetadata params = dimension.getParams();
                assertNotNull(params);

                boolean realTime = dimension.isRealTime();
                assertTrue(realTime);
                Map<String, Object> params1 = new HashMap<>();
                params1.put("deviceId",DEVICE_ID);
                params1.put("history",0);
                MeasurementParameter parameter = MeasurementParameter.of(params1);
                ((Flux<MeasurementValue>)dimension.getValue(parameter)).subscribe();
            }
            if(i==1){
                Constructor constructor = (Constructor) classes[i].getDeclaredConstructor(measurementClass);
                constructor.setAccessible(true);
                dimension = (MeasurementDimension) constructor.newInstance(measurement);

                assertNotNull(dimension);
                DataType valueType = dimension.getValueType();
                assertNotNull(valueType);

                ConfigMetadata params = dimension.getParams();
                assertNotNull(params);

                boolean realTime = dimension.isRealTime();
                assertFalse(realTime);

                Map<String, Object> params1 = new HashMap<>();
                params1.put("deviceId",DEVICE_ID);
                params1.put("history",0);
                MeasurementParameter parameter = MeasurementParameter.of(params1);
                ((Flux<MeasurementValue>)dimension.getValue(parameter)).subscribe();
            }
        }

    }
}