package org.jetlinks.community.device.measurements.message;

import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.dashboard.MeasurementValue;
import org.jetlinks.community.dashboard.SimpleMeasurementValue;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceMessageMeasurementTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    //RealTimeMessageDimension类
    @Test
    void realGetValueType(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.RealTimeMessageDimension realTimeMessageDimension = deviceMessageMeasurement.new RealTimeMessageDimension();
        DataType valueType = realTimeMessageDimension.getValueType();
        assertNotNull(valueType);
        assertEquals(IntType.GLOBAL,valueType);
    }
    @Test
    void realGetParams(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.RealTimeMessageDimension realTimeMessageDimension = deviceMessageMeasurement.new RealTimeMessageDimension();
        ConfigMetadata params = realTimeMessageDimension.getParams();
        assertNotNull(params);
        assertEquals("数据统计周期",params.getProperties().get(0).getName());
    }
    @Test
    void realIsRealTime(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.RealTimeMessageDimension realTimeMessageDimension = deviceMessageMeasurement.new RealTimeMessageDimension();
        boolean realTime = realTimeMessageDimension.isRealTime();
        assertTrue(realTime);
    }
    @Test
    void realGetValue(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.RealTimeMessageDimension realTimeMessageDimension = deviceMessageMeasurement.new RealTimeMessageDimension();
        Map<String, Object> params = new HashMap<>();
        params.put("productId",PRODUCT_ID);
        params.put("msgType",IntType.GLOBAL);
        MeasurementParameter parameter = MeasurementParameter.of(params);
       realTimeMessageDimension.getValue(parameter).subscribe();
    }

    // AggMessageDimension类
    @Test
    void getValueType(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.AggMessageDimension aggMessageDimension = deviceMessageMeasurement.new AggMessageDimension();
        DataType valueType = aggMessageDimension.getValueType();
        assertNotNull(valueType);
        assertEquals(IntType.GLOBAL,valueType);
    }

    @Test
    void getParams(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.AggMessageDimension aggMessageDimension = deviceMessageMeasurement.new AggMessageDimension();
        ConfigMetadata params = aggMessageDimension.getParams();
        assertNotNull(params);
        assertEquals("设备型号",params.getProperties().get(0).getName());
    }

    @Test
    void isRealTime(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.AggMessageDimension aggMessageDimension = deviceMessageMeasurement.new AggMessageDimension();
        boolean realTime = aggMessageDimension.isRealTime();
        assertFalse(realTime);
    }

    @Test
    void createQueryParam() throws Exception {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.AggMessageDimension aggMessageDimension = deviceMessageMeasurement.new AggMessageDimension();

        Class<? extends DeviceMessageMeasurement.AggMessageDimension> aggMessageDimensionClass = aggMessageDimension.getClass();
        Method createQueryParam =
            aggMessageDimensionClass.getDeclaredMethod("createQueryParam", MeasurementParameter.class);
        createQueryParam.setAccessible(true);
        Map<String, Object> params = new HashMap<>();
        MeasurementParameter measurementParameter = MeasurementParameter.of(params);
        createQueryParam.invoke(aggMessageDimension,measurementParameter);
    }

    @Test
    void getProductMetrics() throws Exception {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

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
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));


        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.AggMessageDimension aggMessageDimension = deviceMessageMeasurement.new AggMessageDimension();

        Class<? extends DeviceMessageMeasurement.AggMessageDimension> aggMessageDimensionClass = aggMessageDimension.getClass();
        Method getProductMetrics =
            aggMessageDimensionClass.getDeclaredMethod("getProductMetrics", List.class);
        getProductMetrics.setAccessible(true);

        List<String> list = new ArrayList<>();
        list.add(DEVICE_ID);
        Mono<TimeSeriesMetric[]> invoke = (Mono<TimeSeriesMetric[]>) getProductMetrics.invoke(aggMessageDimension, list);
        invoke.map(t->t.length)
            .as(StepVerifier::create)
            .expectNext(2)
            .verifyComplete();

        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.error(()->new NotFoundException()));
        invoke.map(t->t.length)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void getValue(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(timeSeriesService);

        Mockito.when(timeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(new HashMap<>())));

        DeviceMessageMeasurement deviceMessageMeasurement = new DeviceMessageMeasurement(new BrokerEventBus(),registry,timeSeriesManager);
        DeviceMessageMeasurement.AggMessageDimension aggMessageDimension = deviceMessageMeasurement.new AggMessageDimension();

        Map<String, Object> params = new HashMap<>();
        params.put("productId",PRODUCT_ID);
        params.put("msgType",IntType.GLOBAL);
        MeasurementParameter parameter = MeasurementParameter.of(params);
        aggMessageDimension.getValue(parameter)
            .map(SimpleMeasurementValue::getTimeString)
            .as(StepVerifier::create)
            .expectNext("")
            .verifyComplete();
    }
}