package org.jetlinks.community.device.service.data;

import org.junit.jupiter.api.Test;

//@SpringBootTest(classes ={JetLinksApplication.class})
class DefaultDeviceDataServiceTest {
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

    @Test
    void getStoreStrategy() {
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