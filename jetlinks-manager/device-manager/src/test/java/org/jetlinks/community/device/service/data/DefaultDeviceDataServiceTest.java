package org.jetlinks.community.device.service.data;

import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

//@SpringBootTest(classes ={JetLinksApplication.class})
@WebFluxTest(DefaultDeviceDataService.class)
class DefaultDeviceDataServiceTest extends TestJetLinksController{
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    //@Test
//    void registerMetadata() {
//        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
//        DeviceDataStorageProperties properties = Mockito.mock(DeviceDataStorageProperties.class);
//        ObjectProvider<DeviceDataStoragePolicy> policies = Mockito.mock(ObjectProvider.class);
//        //DefaultListableBeanFactory defaultListableBeanFactory = new DefaultListableBeanFactory();
//        DeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
//        System.out.println(policies.getIfAvailable(() -> noneDeviceDataStoragePolicy).getId());
//
//
//
//        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
//        deviceProductEntity.setId(PRODUCT_ID);
//
//        DeviceProductOperator productOperator = InMemoryDeviceRegistry.create().register(deviceProductEntity.toProductInfo()).block();
//        productOperator.setConfig("storePolicy","none");
//        Mockito.when(registry.getProduct(Mockito.anyString()))
//            .thenReturn(Mono.just(productOperator));
//
//        DefaultDeviceDataService service = new DefaultDeviceDataService(registry, properties,policies);
//        service.registerMetadata(PRODUCT_ID, new SimpleDeviceMetadata())
//            .as(StepVerifier::create)
//            .expectSubscription()
//            .verifyComplete();
//
//    }
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
    void getDeviceStrategy() {
    }

    @Test
    void queryEachOneProperties() {
    }

    @Test
    void queryEachProperties() {
    }

    @Test
    void queryProperty() {
    }

    @Test
    void aggregationPropertiesByProduct() {
    }

    @Test
    void aggregationPropertiesByDevice() {
    }

    @Test
    void queryPropertyPage() {
    }

    @Test
    void queryDeviceMessageLog() {
    }

    @Test
    void saveDeviceMessage() {
    }

    @Test
    void testSaveDeviceMessage() {
    }

    @Test
    void queryEvent() {
    }

    @Test
    void queryEventPage() {
    }
}