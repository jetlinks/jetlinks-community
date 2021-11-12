package org.jetlinks.community.device.message;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultDeviceDataManagerTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";
    @Test
    void newCache() {
        Map<Object, Object> map = DefaultDeviceDataManager.newCache();
        System.out.println(map);
    }

    @Test
    void getLastProperty() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository =Mockito.mock(ReactiveRepository.class);

        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue(DefaultDeviceDataManager.NULL);
        deviceProperty.setTimestamp(System.currentTimeMillis());
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(),Mockito.any(QueryParamEntity.class),Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));

        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);
        manager.getLastProperty(DEVICE_ID,DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext(DefaultDeviceDataManager.NULL)
            .verifyComplete();

        manager.getLastProperty(DEVICE_ID,DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }
    @Test
    void getLastPropert1y() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository =Mockito.mock(ReactiveRepository.class);

        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        deviceProperty.setTimestamp(System.currentTimeMillis()+4000000000L);
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(),Mockito.any(QueryParamEntity.class),Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));

        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);
        manager.getLastProperty(DEVICE_ID,DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        manager.getLastProperty(DEVICE_ID,DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        DeviceProperty deviceProperty1 = new DeviceProperty();
        deviceProperty1.setValue("test");
        deviceProperty1.setTimestamp(System.currentTimeMillis());
        deviceProperty1.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(),Mockito.any(QueryParamEntity.class),Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty1));
        manager.getLastProperty(DEVICE_ID,DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        //TODO
    }
    @Test
    void testGetLastProperty() {
    }

    @Test
    void getLastPropertyTime() {
    }

    @Test
    void getFirstPropertyTime() {
    }

    @Test
    void getFistProperty() {
    }

    @Test
    void getTags() {
    }

    @Test
    void upgradeDeviceFirstPropertyTime() {
    }
}