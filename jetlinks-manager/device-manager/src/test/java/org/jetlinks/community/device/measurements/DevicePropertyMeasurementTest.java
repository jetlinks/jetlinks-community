package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.dashboard.MeasurementDimension;
import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.dashboard.MeasurementValue;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DevicePropertyMeasurementTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void createValue() {
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
        PropertyMetadata propertyMetadata = deviceProductOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
        DevicePropertyMeasurement measurement = new DevicePropertyMeasurement(PRODUCT_ID, new BrokerEventBus(), propertyMetadata, dataService);
        Map<String, Object> value = measurement.createValue("34.5");
        assertNotNull(value);

        measurement.fromHistory(DEVICE_ID,0).subscribe();

        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        Mockito.when(dataService.queryProperty(Mockito.anyString(),Mockito.any(QueryParamEntity.class),Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));
        measurement.fromHistory(DEVICE_ID,1).subscribe();

        EventBus eventBus = Mockito.mock(EventBus.class);
        DevicePropertyMeasurement measurement1 = new DevicePropertyMeasurement(PRODUCT_ID, eventBus, propertyMetadata, dataService);

        ReadPropertyMessageReply readPropertyMessageReply = new ReadPropertyMessageReply();
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature","temperature");
        readPropertyMessageReply.setProperties(properties);
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class),Mockito.any(Class.class)))
            .thenReturn(Flux.just(readPropertyMessageReply));
        measurement1.fromRealTime(DEVICE_ID).subscribe();

    }

    @Test
    void fromHistory() {
    }

    @Test
    void fromRealTime() {
    }

    //内部类
    @Test
    void measurementDimension() throws Exception {
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
        PropertyMetadata propertyMetadata = Mockito.mock(PropertyMetadata.class);
        DevicePropertyMeasurement measurement = new DevicePropertyMeasurement(PRODUCT_ID, new BrokerEventBus(), propertyMetadata, dataService);

        Class<? extends DevicePropertyMeasurement> measurementClass = measurement.getClass();
        Class<?>[] classes = measurementClass.getDeclaredClasses();
        MeasurementDimension dimension = null;
        for (Class<?> aClass : classes) {
            if(aClass.getName().contains("AggDevicePropertyDimension")){
                Mockito.when(propertyMetadata.getId())
                    .thenReturn("test");
                Map<String, Object> map = new HashMap<>();
                map.put("time","07:34:42");
                Mockito.when(dataService.aggregationPropertiesByDevice(Mockito.anyString(),Mockito.any(DeviceDataService.AggregationRequest.class),Mockito.any(DeviceDataService.DevicePropertyAggregation.class)))
                    .thenReturn(Flux.just(AggregationData.of(map)));
                Mockito.when(propertyMetadata.getValueType())
                    .thenReturn(IntType.GLOBAL);
                Constructor constructor = (Constructor) aClass.getDeclaredConstructor(measurementClass);
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
                params1.put("deviceId", DEVICE_ID);
                MeasurementParameter parameter = MeasurementParameter.of(params1);
                Flux<MeasurementValue> value = (Flux<MeasurementValue>) dimension.getValue(parameter);
                value.subscribe();


                Mockito.when(dataService.aggregationPropertiesByProduct(Mockito.anyString(),Mockito.any(DeviceDataService.AggregationRequest.class),Mockito.any(DeviceDataService.DevicePropertyAggregation.class)))
                    .thenReturn(Flux.just(AggregationData.of(map)));
                MeasurementParameter parameter1 = MeasurementParameter.of(new HashMap<>());
                Flux<MeasurementValue> value1 = (Flux<MeasurementValue>) dimension.getValue(parameter1);
                value1.subscribe();
            }

            if(aClass.getName().contains("HistoryDevicePropertyDimension")){
                Mockito.when(propertyMetadata.getId())
                    .thenReturn("test");

                Constructor constructor = (Constructor) aClass.getDeclaredConstructor(measurementClass);
                constructor.setAccessible(true);
                dimension = (MeasurementDimension) constructor.newInstance(measurement);
                assertNotNull(dimension);
                DataType valueType = dimension.getValueType();
                assertNotNull(valueType);

                ConfigMetadata params = dimension.getParams();
                assertNotNull(params);

                boolean realTime = dimension.isRealTime();
                assertFalse(realTime);


                DeviceProperty deviceProperty = new DeviceProperty();
                deviceProperty.setValue("test");
                Mockito.when(dataService.queryProperty(Mockito.anyString(),Mockito.any(QueryParamEntity.class),Mockito.anyString()))
                    .thenReturn(Flux.just(deviceProperty));

                Map<String, Object> params1 = new HashMap<>();
                params1.put("deviceId", DEVICE_ID);
                params1.put("history", 1);
                MeasurementParameter parameter = MeasurementParameter.of(params1);
                Flux<MeasurementValue> value = (Flux<MeasurementValue>) dimension.getValue(parameter);
                value.subscribe();
            }

            if(aClass.getName().contains("RealTimeDevicePropertyDimension")){

                Constructor constructor = (Constructor) aClass.getDeclaredConstructor(measurementClass);
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
                params1.put("deviceId", DEVICE_ID);
                params1.put("history", 0);
                MeasurementParameter parameter = MeasurementParameter.of(params1);
                Flux<MeasurementValue> value = (Flux<MeasurementValue>) dimension.getValue(parameter);
                value.subscribe();
            }
        }


    }
}