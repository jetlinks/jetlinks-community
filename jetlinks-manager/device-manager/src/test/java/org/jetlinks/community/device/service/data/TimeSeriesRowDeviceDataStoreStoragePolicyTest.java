package org.jetlinks.community.device.service.data;


import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesService;
import org.jetlinks.community.timeseries.SimpleTimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.micrometer.MeterTimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.reactivestreams.Publisher;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesRowDeviceDataStoreStoragePolicyTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";
    @Test
    void doSaveData() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);

        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);
        Mockito.when(elasticSearchTimeSeriesService.commit(Mockito.any(TimeSeriesData.class)))
            .thenReturn(Mono.empty().then());

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        SimpleTimeSeriesData simpleTimeSeriesData = new SimpleTimeSeriesData(111111L, new HashMap<>());
        storagePolicy.doSaveData("service", simpleTimeSeriesData)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void doSaveData1() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);

        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);
        Mockito.when(elasticSearchTimeSeriesService.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.empty().then());

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        SimpleTimeSeriesData simpleTimeSeriesData = new SimpleTimeSeriesData(111111L, new HashMap<>());
        storagePolicy.doSaveData("service", Flux.just(simpleTimeSeriesData))
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void doQuery(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);

        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(new SimpleTimeSeriesData(111111L, new HashMap<>())));

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        storagePolicy.doQuery("service", new QueryParamEntity(),(data)->{return "test";})
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }

    @Test
    void doQueryPager(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);

        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);
        Mockito.when(elasticSearchTimeSeriesService.queryPager(Mockito.any(QueryParam.class),Mockito.any(Function.class)))
            .thenReturn(Mono.just(PagerResult.of(1,new ArrayList<String>())));

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        storagePolicy.doQueryPager("service", new QueryParamEntity(),(data)->{return "test";})
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
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
        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("property","temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));
        System.out.println(meterTimeSeriesData.getString("property").get());

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        storagePolicy.queryEachOneProperties(DEVICE_ID, new QueryParamEntity(),"temperature")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();

        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}},{\"id\":\"temperature1\",\"name\":\"温度1\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );
        InMemoryDeviceRegistry inMemoryDeviceRegistry1 = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry1.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator1 = inMemoryDeviceRegistry1.register(deviceInstanceEntity.toDeviceInfo()).block();
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator1));
        Map<String, Object> map1 = new HashMap<>();
        map1.put("property","temperature");
        Mockito.when(elasticSearchTimeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(map1)));
        storagePolicy.queryEachOneProperties(DEVICE_ID, new QueryParamEntity(),"temperature","temperature1")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();

        //TODO 为空
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );
        InMemoryDeviceRegistry inMemoryDeviceRegistry2 = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry2.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator2 = inMemoryDeviceRegistry2.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator2));
        storagePolicy.queryEachOneProperties(DEVICE_ID, new QueryParamEntity(),"").subscribe();
        storagePolicy.queryEachOneProperties(DEVICE_ID, new QueryParamEntity()).subscribe();


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

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
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

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
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
//        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}");


        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(elasticSearchTimeSeriesService);

        Map<String, Object> map1 = new HashMap<>();
        map1.put("property","temperature");
        AggregationData of = AggregationData.of(map1);
        Mockito.when(elasticSearchTimeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(of));


        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        storagePolicy.queryEachProperties(DEVICE_ID,new QueryParamEntity(), "temperature")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();

        storagePolicy.queryEachProperties(DEVICE_ID, new QueryParamEntity(),"aa").subscribe();


    }

    @Test
    void aggregationPropertiesByProduct() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        Map<String, Object> map=new HashMap<>();
        map.put("time","1234444");
        Mockito.when(elasticSearchTimeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(map)));

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        DeviceDataService.AggregationRequest aggregationRequest = new DeviceDataService.AggregationRequest();
        DeviceDataService.DevicePropertyAggregation aggregation = new DeviceDataService.DevicePropertyAggregation();
        aggregation.setAlias("温度");
        aggregation.setProperty("temperature");
        DeviceDataService.DevicePropertyAggregation aggregation1 = new DeviceDataService.DevicePropertyAggregation();
        aggregation1.setAlias("wendu");
        aggregation1.setProperty("temperature1");
        storagePolicy.aggregationPropertiesByProduct(PRODUCT_ID,aggregationRequest,aggregation,aggregation1)
            .map(s->s.getString("time").get())
            .as(StepVerifier::create)
            .expectNext("1234444")
            .verifyComplete();
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
        map1.put("time","1234444");
        Mockito.when(elasticSearchTimeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(map1)));

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
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
            .expectNext("1234444")
            .verifyComplete();
    }

    @Test
    void convertProperties(){
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

        Map<String, Object> map1=new HashMap<>();
        map1.put("time","1234444");
        Mockito.when(elasticSearchTimeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(map1)));

        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

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
        properties1.put("test","36");
        storagePolicy.convertProperties(PRODUCT_ID,message,properties1)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("temperature","test");
        storagePolicy.convertProperties(PRODUCT_ID,message,properties2)
            .as(StepVerifier::create)
            .expectError(UnsupportedOperationException.class)
            .verify();

        Map<String, Object> properties3 = new HashMap<>();
        properties3.put("temperature",null);
        storagePolicy.convertProperties(PRODUCT_ID,message,properties3)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("properties_"+PRODUCT_ID)
            .verifyComplete();

        message.addHeader(Headers.useTimestampAsId.getKey(),true);
        storagePolicy.convertProperties(PRODUCT_ID,message,properties)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("properties_"+PRODUCT_ID)
            .verifyComplete();
        storagePolicy.convertProperties(PRODUCT_ID,message,new HashMap<>()).subscribe();

        //TODO 其他类型AbstractDeviceDataStoragePolicy
    }

    @Test
    void convertPropertyValue() {
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
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));
        PropertyMetadata propertyMetadata = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
//        propertyMetadata.getValueType();
        Map<String, Object> expands = new HashMap<>();
        expands.put("storageType", "json-string");
        propertyMetadata.setExpands(expands);
        TimeSeriesRowDeviceDataStoreStoragePolicy storagePolicy = new TimeSeriesRowDeviceDataStoreStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        Object o = storagePolicy.convertPropertyValue(null, null);
        assertNull(o);

        Object test = storagePolicy.convertPropertyValue("test", propertyMetadata);
        assertEquals("test", test);
        expands.put("storageType", "aa");
        System.out.println(propertyMetadata.getValueType());
        Object value = storagePolicy.convertPropertyValue("test", propertyMetadata);
        assertNull(value);

        deviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"unknown\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}").subscribe();
        PropertyMetadata propertyMetadata1 = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();

        Object value1 = storagePolicy.convertPropertyValue("test", propertyMetadata1);
        assertEquals("test", value1);
    }
}