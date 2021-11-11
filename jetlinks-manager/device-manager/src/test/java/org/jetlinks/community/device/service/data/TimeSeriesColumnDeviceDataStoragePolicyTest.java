package org.jetlinks.community.device.service.data;


import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.micrometer.MeterTimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceLogMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.SimpleDeviceMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.supports.official.JetLinksEventMetadata;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesColumnDeviceDataStoragePolicyTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";
    @Test
    void registerMetadata(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

        Mockito.when(timeSeriesManager.registerMetadata(Mockito.any(TimeSeriesMetadata.class)))
            .thenReturn(Mono.just(1).then());

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());
        SimpleDeviceMetadata simpleDeviceMetadata = new SimpleDeviceMetadata();
        simpleDeviceMetadata.addEvent(new JetLinksEventMetadata("test","test", IntType.GLOBAL));
        storagePolicy.registerMetadata(PRODUCT_ID,simpleDeviceMetadata)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void queryEachOneProperties() {
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

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();


        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature","temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));
//        System.out.println(meterTimeSeriesData.getString("temperature").get());


        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());
        storagePolicy.queryEachOneProperties(DEVICE_ID,new  QueryParamEntity(),"temperature")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();
        storagePolicy.queryEachOneProperties(DEVICE_ID,new  QueryParamEntity()).subscribe();
    }

    @Test
    void queryPropertyPage() {
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
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(elasticSearchTimeSeriesService);
        Mockito.when(elasticSearchTimeSeriesService.queryPager(Mockito.any(QueryParam.class),Mockito.any(Function.class)))
            .thenReturn(Mono.just(PagerResult.of(1,new ArrayList<>())));


        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());
        storagePolicy.queryPropertyPage(DEVICE_ID, "temperature",new QueryParamEntity())
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

    }

    @Test
    void queryProperty() {
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
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("property","temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());
        storagePolicy.queryProperty(DEVICE_ID,new QueryParamEntity(), "temperature")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();
    }

    @Test
    void queryEachProperties() {
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


        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.any(String.class)))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature","temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));


        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());

        storagePolicy.queryEachProperties(DEVICE_ID,new QueryParamEntity(), "temperature")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();

    }

    @Test
    void aggregationPropertiesByProduct() {

    }

    @Test
    void aggregationPropertiesByDevice() {
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
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));


        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        Map<String, Object> map1=new HashMap<>();
        map1.put("time","2018-04-23");
        Map<String, Object> map2=new HashMap<>();
        map2.put("time","2018-04-22");
        Map<String, Object> map3=new HashMap<>();
        map3.put("time","2018-04-23");
        Mockito.when(elasticSearchTimeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(map1),AggregationData.of(map2),AggregationData.of(map3)));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());
        DeviceDataService.AggregationRequest aggregationRequest = new DeviceDataService.AggregationRequest();
        DeviceDataService.DevicePropertyAggregation aggregation = new DeviceDataService.DevicePropertyAggregation();
        aggregation.setAlias("温度");
        aggregation.setProperty("temperature");
        DeviceDataService.DevicePropertyAggregation aggregation1 = new DeviceDataService.DevicePropertyAggregation();
        aggregation1.setAlias("wendu");
        aggregation1.setProperty("temperature1");
        storagePolicy.aggregationPropertiesByDevice(DEVICE_ID,aggregationRequest,aggregation,aggregation1)
            .map(s->s.getString("time").get())
            .as(StepVerifier::create)
            .expectNext("2018-04-23")
            .expectNext("2018-04-22")
            .verifyComplete();

        aggregationRequest.setInterval(null);
        storagePolicy.aggregationPropertiesByDevice(DEVICE_ID,aggregationRequest,aggregation,aggregation1)
            .map(s->s.getString("time").get())
            .as(StepVerifier::create)
            .expectNext("2018-04-23")
            .expectNext("2018-04-22")
            .verifyComplete();
    }

    @Test
    void convertProperties() {
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
        Map<String, Object> map2 = new HashMap<>();
        map2.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map2);

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature","temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());

        Map<String, Long> map=new HashMap<>();
        map.put("time",111111111L);
        ReportPropertyMessage message = ReportPropertyMessage.create();
        message.setPropertySourceTimes(map);
        message.setDeviceId(DEVICE_ID);
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature","36");
        message.setTimestamp(1111111111111111L);
        storagePolicy.convertProperties(PRODUCT_ID,message,properties)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("properties_"+PRODUCT_ID)
            .verifyComplete();

        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("aaa",null);
        storagePolicy.convertProperties(PRODUCT_ID,message,properties1)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        ReportPropertyMessage message1 = ReportPropertyMessage.create();
        message1.setPropertySourceTimes(map);
        message1.setDeviceId(DEVICE_ID);
        message1.addHeader(Headers.useTimestampAsId.getKey(),true);
        message1.addHeader(Headers.partialProperties.getKey(),true);
        storagePolicy.convertProperties(PRODUCT_ID,message1,properties)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("properties_"+PRODUCT_ID)
            .verifyComplete();

        storagePolicy.convertProperties(PRODUCT_ID,message,new HashMap<>()).subscribe();
    }


//    =======AbstractDeviceDataStoragePolicy父类方法=================

    @Test
    void saveDeviceMessage(){
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
        Map<String, Object> map2 = new HashMap<>();
        map2.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map2);

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature","temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,new DeviceDataStorageProperties());

        ReportPropertyMessage message = ReportPropertyMessage.create();
        Map<String, Long> map=new HashMap<>();
        map.put("time",111111111L);
        message.setPropertySourceTimes(map);
        message.setDeviceId(DEVICE_ID);
        message.addHeader(Headers.ignoreStorage.getKey(),true);
        message.addHeader(Headers.ignoreLog.getKey(),true);
        storagePolicy.saveDeviceMessage(message)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        message.addHeader("productId",PRODUCT_ID);
        message.addHeader(Headers.ignoreStorage.getKey(),false);
        message.addHeader(Headers.ignoreLog.getKey(),true);
        message.addHeader(Headers.useTimestampAsId.getKey(),true);
        message.addHeader(Headers.partialProperties.getKey(),true);
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature","36");
        message.setProperties(properties);
        storagePolicy.saveDeviceMessage(message);


        EventMessage eventMessage = new EventMessage();
        eventMessage.setEvent("event");
        eventMessage.setData("aaa");
        storagePolicy.saveDeviceMessage(eventMessage);

        //TODO
        DeviceLogMessage deviceLogMessage = new DeviceLogMessage();
        deviceLogMessage.setLog("log");
        deviceLogMessage.addHeader("productId",PRODUCT_ID);
        deviceLogMessage.addHeader(Headers.ignoreStorage.getKey(),true);
        deviceLogMessage.addHeader(Headers.ignoreLog.getKey(),false);
        storagePolicy.saveDeviceMessage(deviceLogMessage);

    }

}