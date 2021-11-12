package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceLogMessage;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@WebFluxTest(DefaultDeviceDataService.class)
class DefaultDeviceDataServiceTest extends TestJetLinksController{
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Autowired
    private ObjectProvider<DeviceDataStoragePolicy> policies;

    @Test
    void getStoreStrategy() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);

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

        DeviceProductOperator deviceProductOperator = InMemoryDeviceRegistry.create().register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig("storePolicy","none").subscribe();
        deviceProductOperator.getConfig("storePolicy").subscribe();

        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DefaultDeviceDataService defaultDeviceDataService = new DefaultDeviceDataService(registry, properties, policies);

        defaultDeviceDataService.getStoreStrategy(PRODUCT_ID)
            .map(DeviceDataStoragePolicy::getName)
            .as(StepVerifier::create)
            .expectNext("不存储")
            .verifyComplete();


    }


    @Test
    void queryProperty() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);
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

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig("storePolicy","none").subscribe();
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DefaultDeviceDataService defaultDeviceDataService = new DefaultDeviceDataService(registry, properties, policies);
        defaultDeviceDataService.queryProperty(DEVICE_ID,new QueryParamEntity(),"test")
            .as(StepVerifier::create)
            .expectComplete()
            .verify();

    }



    @Test
    void saveDeviceMessage() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);
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

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig("storePolicy","none").subscribe();
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DefaultDeviceDataService defaultDeviceDataService = new DefaultDeviceDataService(registry, properties, policies);
        DeviceLogMessage deviceLogMessage = new DeviceLogMessage();
        deviceLogMessage.setDeviceId(DEVICE_ID);
        defaultDeviceDataService.saveDeviceMessage(deviceLogMessage)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }

    @Test
    void testSaveDeviceMessage() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);
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

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig("storePolicy","none").subscribe();
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DefaultDeviceDataService defaultDeviceDataService = new DefaultDeviceDataService(registry, properties, policies);
        DeviceLogMessage deviceLogMessage = new DeviceLogMessage();
        deviceLogMessage.setDeviceId(DEVICE_ID);
        defaultDeviceDataService.saveDeviceMessage(Mono.just(deviceLogMessage))
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }

    @Test
    void queryEvent() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);
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

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig("storePolicy","none").subscribe();
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DefaultDeviceDataService defaultDeviceDataService = new DefaultDeviceDataService(registry, properties, policies);
        defaultDeviceDataService.queryEvent(DEVICE_ID,"event",new QueryParamEntity(),true)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }

    /*=============DeviceDataService==============*/
    @Test
    void saveDeviceMessage2(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);

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

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig("storePolicy","none").subscribe();
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DeviceDataService defaultDeviceDataService = new DefaultDeviceDataService(registry, properties, policies);

        DeviceLogMessage deviceLogMessage = new DeviceLogMessage();
        deviceLogMessage.setDeviceId(DEVICE_ID);
        ArrayList<DeviceMessage> list = new ArrayList<>();
        defaultDeviceDataService.saveDeviceMessage(list)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }


    @Test
    void queryEventPage(){
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);

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

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig("storePolicy","none").subscribe();
        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DeviceDataService defaultDeviceDataService = new DefaultDeviceDataService(registry, properties, policies);
        defaultDeviceDataService.queryEventPage(DEVICE_ID,"event",new QueryParamEntity())
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(0)
            .verifyComplete();
    }
}