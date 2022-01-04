package org.jetlinks.community.device.service.data;


import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesService;
import org.jetlinks.community.timeseries.*;
import org.jetlinks.community.timeseries.micrometer.MeterTimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.DeviceLogMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimpleDeviceMetadata;
import org.jetlinks.core.metadata.types.GeoPoint;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.supports.official.JetLinksEventMetadata;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesColumnDeviceDataStoragePolicyTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void registerMetadata() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

        Mockito.when(timeSeriesManager.registerMetadata(Mockito.any(TimeSeriesMetadata.class)))
            .thenReturn(Mono.just(1).then());

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        SimpleDeviceMetadata simpleDeviceMetadata = new SimpleDeviceMetadata();
        simpleDeviceMetadata.addEvent(new JetLinksEventMetadata("test", "test", IntType.GLOBAL));
        storagePolicy.registerMetadata(PRODUCT_ID, simpleDeviceMetadata)
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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature", "temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));


        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        storagePolicy.queryEachOneProperties(DEVICE_ID, new QueryParamEntity(), "temperature")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();

        storagePolicy.queryEachOneProperties(DEVICE_ID, new QueryParamEntity())
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();

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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));


        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(timeSeriesService);

        Mockito.when(timeSeriesService.count(Mockito.any(QueryParam.class)))
            .thenReturn(Mono.just(1));
        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("id", "temperature");

        Mockito.when(timeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        Mockito.when(timeSeriesService.queryPager(Mockito.any(QueryParam.class), Mockito.any(Function.class)))
            .thenCallRealMethod()
            .thenReturn(Mono.just(PagerResult.of(1, new ArrayList<>())));


        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        storagePolicy.queryPropertyPage(DEVICE_ID, "temperature", new QueryParamEntity())
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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("property", "temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        storagePolicy.queryProperty(DEVICE_ID, new QueryParamEntity(), "temperature")
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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.any(String.class)))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature", "temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));


        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        storagePolicy.queryEachProperties(DEVICE_ID, new QueryParamEntity(), "temperature")
            .map(DeviceProperty::getPropertyName)
            .as(StepVerifier::create)
            .expectNext("温度")
            .verifyComplete();

    }


    @Test
    void aggregationPropertiesByDevice() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.addConfig(DeviceConfigKey.productId.getKey(),"productId")
            .addConfig(DeviceConfigKey.productVersion.getKey(),"0.1");

        Mono<DeviceProductOperator> deviceProductOperatorMono = inMemoryDeviceRegistry.register(productInfo);
        assertNotNull(deviceProductOperatorMono);
        DeviceOperator operator = Mockito.mock(DeviceOperator.class);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(operator));
        Mockito.when(operator.getProduct())
            .thenReturn(deviceProductOperatorMono);


        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        Map<String, Object> map1 = new HashMap<>();
        map1.put("time", "2018-04-23");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("time", "2018-04-22");
        Map<String, Object> map3 = new HashMap<>();
        map3.put("time", "2018-04-23");
        Mockito.when(elasticSearchTimeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(map1), AggregationData.of(map2), AggregationData.of(map3)));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        DeviceDataService.AggregationRequest aggregationRequest = new DeviceDataService.AggregationRequest();
        DeviceDataService.DevicePropertyAggregation aggregation = new DeviceDataService.DevicePropertyAggregation();
        aggregation.setAlias("温度");
        aggregation.setProperty("temperature");
        DeviceDataService.DevicePropertyAggregation aggregation1 = new DeviceDataService.DevicePropertyAggregation();
        aggregation1.setAlias("wendu");
        aggregation1.setProperty("temperature1");
        storagePolicy.aggregationPropertiesByDevice(DEVICE_ID, aggregationRequest, aggregation, aggregation1)
            .map(s -> s.getString("time").get())
            .as(StepVerifier::create)
            .expectNext("2018-04-23")
            .expectNext("2018-04-22")
            .verifyComplete();

        aggregationRequest.setInterval(null);
        storagePolicy.aggregationPropertiesByDevice(DEVICE_ID, aggregationRequest, aggregation, aggregation1)
            .map(s -> s.getString("time").get())
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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature", "temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        assertNotNull(storagePolicy);
        Map<String, Long> map = new HashMap<>();
        map.put("time", 111111111L);
        ReportPropertyMessage message = ReportPropertyMessage.create();
        message.setPropertySourceTimes(map);
        message.setDeviceId(DEVICE_ID);
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature", "36");
        message.setTimestamp(1111111111111111L);
        assertNotNull(storagePolicy);
        storagePolicy.convertProperties(PRODUCT_ID, message, properties)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("properties_" + PRODUCT_ID)
            .verifyComplete();

        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("aaa", null);
        storagePolicy.convertProperties(PRODUCT_ID, message, properties1)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        ReportPropertyMessage message1 = ReportPropertyMessage.create();
        message1.setPropertySourceTimes(map);
        message1.setDeviceId(DEVICE_ID);
        message1.addHeader(Headers.useTimestampAsId.getKey(), true);
        message1.addHeader(Headers.partialProperties.getKey(), true);
        storagePolicy.convertProperties(PRODUCT_ID, message1, properties)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("properties_" + PRODUCT_ID)
            .verifyComplete();

        storagePolicy.convertProperties(PRODUCT_ID, message, new HashMap<>()).subscribe();


    }


//    =======AbstractDeviceDataStoragePolicy父类方法=================

    @Test
    void saveDeviceMessage() {
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

        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature", "temperature");
        Mockito.when(elasticSearchTimeSeriesService.commit(Mockito.any(TimeSeriesData.class)))
            .thenReturn(Mono.just(1).then());

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        assertNotNull(storagePolicy);

        ReportPropertyMessage message = ReportPropertyMessage.create();
        Map<String, Long> map = new HashMap<>();
        map.put("time", 111111111L);
        message.setPropertySourceTimes(map);
        message.setDeviceId(DEVICE_ID);
        message.addHeader(Headers.ignoreStorage.getKey(), true);
        message.addHeader(Headers.ignoreLog.getKey(), true);
        storagePolicy.saveDeviceMessage(message)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        message.addHeader("productId", PRODUCT_ID);
        message.addHeader(Headers.ignoreStorage.getKey(), false);
        message.addHeader(Headers.ignoreLog.getKey(), true);
        message.addHeader(Headers.useTimestampAsId.getKey(), true);
        message.addHeader(Headers.partialProperties.getKey(), true);
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature", "36");
        message.setProperties(properties);
        storagePolicy.saveDeviceMessage(message);
//        EventMessage eventMessage = new EventMessage();
//        eventMessage.setEvent("fire_alarm");
//        eventMessage.setData("aaa");
//
//        deviceProductOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}").subscribe(System.out::println);
//      Mockito.when(registry.getProduct(Mockito.anyString()))
//            .thenReturn(Mono.just(deviceProductOperator));
//
//        storagePolicy.saveDeviceMessage(eventMessage);
//
//


    }

    @Test
    void saveDeviceMessage1() {
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
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature", "temperature");
        Mockito.when(elasticSearchTimeSeriesService.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1).then());

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        ReportPropertyMessage message = ReportPropertyMessage.create();
        Map<String, Long> map = new HashMap<>();
        map.put("time", 111111111L);
        message.setPropertySourceTimes(map);
        message.setDeviceId(DEVICE_ID);
        message.addHeader(Headers.ignoreStorage.getKey(), true);
        message.addHeader(Headers.ignoreLog.getKey(), true);
        storagePolicy.saveDeviceMessage(Mono.just(message))
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
        message.addHeader("productId", PRODUCT_ID);
        message.addHeader(Headers.ignoreStorage.getKey(), false);
        message.addHeader(Headers.ignoreLog.getKey(), true);
        message.addHeader(Headers.useTimestampAsId.getKey(), true);
        message.addHeader(Headers.partialProperties.getKey(), true);
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature", "36");
        message.setProperties(properties);
        storagePolicy.saveDeviceMessage(Mono.just(message));
    }

    @Test
    void getDeviceLogMetric() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        String deviceLogMetric = storagePolicy.getDeviceLogMetric(PRODUCT_ID);
        assertNotNull(deviceLogMetric);
        assertEquals("device_log_" + PRODUCT_ID, deviceLogMetric);
    }

    @Test
    void getDeviceEventMetric() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        assertNotNull(storagePolicy);
        String deviceEventMetric = storagePolicy.getDeviceEventMetric(PRODUCT_ID, "aa");
        assertNotNull(deviceEventMetric);
        assertEquals("event_" + PRODUCT_ID + "_aa", deviceEventMetric);
    }

    @Test
    void createDeviceMessageLog() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        assertNotNull(storagePolicy);
        DeviceLogMessage deviceLogMessage = new DeviceLogMessage();
        deviceLogMessage.setLog("log");
        deviceLogMessage.setDeviceId(DEVICE_ID);
        deviceLogMessage.setMessageId("test");
        deviceLogMessage.addHeader("log", "test_log");
        storagePolicy.createDeviceMessageLog(PRODUCT_ID, deviceLogMessage, (msg, log) -> log.setContent(((DeviceLogMessage) msg).getLog()))
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("device_log_" + PRODUCT_ID)
            .verifyComplete();

    }

    @Test
    void convertMessageToTimeSeriesData() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
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

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        assertNotNull(deviceProductOperator);

        deviceProductOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}").subscribe();

        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));


        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        assertNotNull(storagePolicy);
        EventMessage eventMessage = new EventMessage();
        eventMessage.setEvent("fire_alarm");
        eventMessage.addHeader(Headers.ignoreStorage.getKey(), false);
        eventMessage.addHeader(Headers.ignoreLog.getKey(), true);
        eventMessage.addHeader("productId", PRODUCT_ID);
        storagePolicy.convertMessageToTimeSeriesData(eventMessage)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("event_test100_fire_alarm")
            .verifyComplete();

        eventMessage.setData(new HashMap<>());
        storagePolicy.convertMessageToTimeSeriesData(eventMessage)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("event_test100_fire_alarm")
            .verifyComplete();


        deviceProductOperator.updateMetadata("{\"events\":[],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}").subscribe();
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));
        storagePolicy.convertMessageToTimeSeriesData(eventMessage)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
        eventMessage.addHeader(Headers.ignoreStorage.getKey(), true);
        eventMessage.addHeader(Headers.ignoreLog.getKey(), false);
        storagePolicy.convertMessageToTimeSeriesData(eventMessage)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("device_log_" + PRODUCT_ID)
            .verifyComplete();

        DeviceLogMessage deviceLogMessage = new DeviceLogMessage();
        deviceLogMessage.setLog("log");
        deviceLogMessage.addHeader(Headers.ignoreStorage.getKey(), true);
        deviceLogMessage.addHeader(Headers.ignoreLog.getKey(), false);
        deviceLogMessage.addHeader("productId", PRODUCT_ID);
        storagePolicy.convertMessageToTimeSeriesData(deviceLogMessage)
            .map(Tuple2::getT1)
            .as(StepVerifier::create)
            .expectNext("device_log_" + PRODUCT_ID)
            .verifyComplete();

    }

    @Test
    void queryDeviceMessageLog() {
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

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        //inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(deviceOperator);
        deviceOperator.setConfig(DeviceConfigKey.productId, PRODUCT_ID).subscribe();
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(timeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("id", "temperature");

        Mockito.when(timeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));
        Mockito.when(timeSeriesService.count(Mockito.any(QueryParam.class)))
            .thenReturn(Mono.just(1));

        List<DeviceOperationLogEntity> list = new ArrayList<>();
        DeviceOperationLogEntity deviceOperationLogEntity = new DeviceOperationLogEntity();
        deviceOperationLogEntity.setId("test");
        deviceOperationLogEntity.setDeviceId(DEVICE_ID);
        list.add(deviceOperationLogEntity);
        Mockito.when(timeSeriesService.queryPager(Mockito.any(QueryParam.class), Mockito.any(Function.class)))
            .thenCallRealMethod()
            .thenReturn(Mono.just(PagerResult.of(1, list)));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        storagePolicy.queryDeviceMessageLog(DEVICE_ID, new QueryParamEntity())
//            .map(PagerResult::getTotal)
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void queryEvent() {

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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ElasticSearchTimeSeriesService elasticSearchTimeSeriesService = Mockito.mock(ElasticSearchTimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(elasticSearchTimeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature", "temperature");
        Mockito.when(elasticSearchTimeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        storagePolicy.queryEvent(DEVICE_ID, "fire_alarm", new QueryParamEntity(), true)
            .map(DeviceEvent::isEmpty)
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void queryEventPage() {

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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);
        Mockito.when(timeSeriesManager.getService(Mockito.anyString()))
            .thenReturn(timeSeriesService);

        MeterTimeSeriesData meterTimeSeriesData = new MeterTimeSeriesData();
        meterTimeSeriesData.getData().put("temperature", "temperature");
        Mockito.when(timeSeriesService.query(Mockito.any(QueryParam.class)))
            .thenReturn(Flux.just(meterTimeSeriesData));

        Mockito.when(timeSeriesService.count(Mockito.any(QueryParam.class)))
            .thenReturn(Mono.just(1));

        List<DeviceOperationLogEntity> list = new ArrayList<>();
        DeviceOperationLogEntity deviceOperationLogEntity = new DeviceOperationLogEntity();
        deviceOperationLogEntity.setId("test");
        deviceOperationLogEntity.setDeviceId(DEVICE_ID);
        list.add(deviceOperationLogEntity);
        Mockito.when(timeSeriesService.queryPager(Mockito.any(QueryParam.class), Mockito.any(Function.class)))
            .thenCallRealMethod()
            .thenReturn(Mono.just(PagerResult.of(1, list)));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        storagePolicy.queryEventPage(DEVICE_ID, "fire_alarm", new QueryParamEntity(), true)
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();

        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));
        PropertyMetadata propertyMetadata = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());

        assertNotNull(storagePolicy);
        Object o = storagePolicy.convertPropertyValue(null, null);
        assertNull(o);

        Object test = storagePolicy.convertPropertyValue("test", propertyMetadata);
        assertEquals("test", test);

        deviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"unknown\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}").subscribe();
        PropertyMetadata propertyMetadata1 = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();

        Object value1 = storagePolicy.convertPropertyValue("test", propertyMetadata1);
        assertEquals("test", value1);
    }

    @Test
    void fillRowPropertyValue() {
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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        storagePolicy.fillRowPropertyValue(new HashMap<>(), null, 1);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), null, new Date());
        storagePolicy.fillRowPropertyValue(new HashMap<>(), null, "test");
        deviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"date\",\"format\":\"yyyy-MM-dd\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();
        PropertyMetadata propertyMetadata = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
        assertNotNull(propertyMetadata);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), propertyMetadata, "2020-02-01");

        //对象
        deviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"object\",\"properties\":[]},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();
        PropertyMetadata propertyMetadata1 = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
        assertNotNull(propertyMetadata1);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), propertyMetadata1, "{'test':'test'}");

        //数组
        deviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"array\",\"elementType\":{\"type\":\"int\"}},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();
        PropertyMetadata propertyMetadata2 = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
        assertNotNull(propertyMetadata2);
        ArrayList<Integer> list = new ArrayList<>();
        list.add(100);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), propertyMetadata2, list);

        //GeoType
        deviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"geoPoint\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();
        PropertyMetadata propertyMetadata3 = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
        assertNotNull(propertyMetadata3);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), propertyMetadata3, GeoPoint.of(100.00, 120.00));

        deviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"string\",\"expands\":{}},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();
        PropertyMetadata propertyMetadata4 = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
        assertNotNull(propertyMetadata4);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), propertyMetadata4, "100");

        Map<String, Object> expands = new HashMap<>();
        expands.put("storageType", "json-string");
        propertyMetadata3.setExpands(expands);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), propertyMetadata3, GeoPoint.of(100.00, 120.00));

        PropertyMetadata propertyMetadata5 = deviceOperator.getMetadata().map(s -> s.getProperties().get(0)).block();
        assertNotNull(propertyMetadata5);
        propertyMetadata5.setExpands(expands);
        storagePolicy.fillRowPropertyValue(new HashMap<>(), propertyMetadata5, "100");
    }

    @Test
    void getProductAndMetadataByProduct(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
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
        //inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        //DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        assertNotNull(deviceProductOperator);
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        storagePolicy.getProductAndMetadataByProduct(PRODUCT_ID)
            .map(s->s.getT2().getProperties().get(0))
            .map(s->s.getValueType().getType())
            .as(StepVerifier::create)
            .expectNext("float")
            .verifyComplete();
    }

    @Test
    void getPropertyMetadata(){
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
        ProductInfo productInfo = deviceProductEntity.toProductInfo();
        productInfo.setVersion("1.1.9");
        inMemoryDeviceRegistry.register(productInfo).subscribe();
        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID)
            .addConfig(DeviceConfigKey.productVersion.getKey(), "1.1.9")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);
        DeviceMetadata deviceMetadata = deviceOperator.getMetadata().block();
        assertNotNull(deviceMetadata);
        TimeSeriesColumnDeviceDataStoragePolicy storagePolicy = new TimeSeriesColumnDeviceDataStoragePolicy(registry, timeSeriesManager, new DeviceDataStorageProperties());
        assertNotNull(storagePolicy);
        String type = storagePolicy.getPropertyMetadata(deviceMetadata, null).get(0).getValueType().getType();
        assertEquals("float",type);


        String type1 = storagePolicy.getPropertyMetadata(deviceMetadata, "temperature","test").get(0).getValueType().getType();
        assertEquals("float",type1);

    }

}