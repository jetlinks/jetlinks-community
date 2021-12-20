package org.jetlinks.community.device.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.core.device.*;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AutoDiscoverDeviceRegistryTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void getDevice() {
        DeviceRegistry parent = Mockito.mock(DeviceRegistry.class);
        ReactiveRepository<DeviceInstanceEntity, String> deviceRepository = Mockito.mock(ReactiveRepository.class);
        ReactiveRepository<DeviceProductEntity, String> productRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);

        Mockito.when(parent.getDevice(Mockito.anyString()))
            .thenReturn(InMemoryDeviceRegistry.create().register(deviceInstanceEntity.toDeviceInfo()));


        AutoDiscoverDeviceRegistry service = new AutoDiscoverDeviceRegistry(parent, deviceRepository, productRepository);
        service.getDevice(DEVICE_ID)
            .map(DeviceOperator::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();

        Mockito.when(parent.getDevice(Mockito.anyString()))
            .thenReturn(Mono.empty());
        deviceInstanceEntity.setState(DeviceState.online);
        Mockito.when(deviceRepository.findById(Mockito.anyString()))
            .thenReturn(Mono.just(deviceInstanceEntity));
        Mockito.when(parent.register(Mockito.any(DeviceInfo.class)))
            .thenReturn(InMemoryDeviceRegistry.create().register(deviceInstanceEntity.toDeviceInfo()));

        service.getDevice(DEVICE_ID)
            .map(DeviceOperator::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();
        service.getDevice("")
            .map(DeviceOperator::getDeviceId)
            .switchIfEmpty(Mono.error(new NotFoundException()))
            .onErrorResume(e->Mono.just("设备id为空"))
            .as(StepVerifier::create)
            .expectNext("设备id为空")
            .verifyComplete();

    }

    @Test
    void getProduct() {
        DeviceRegistry parent = Mockito.mock(DeviceRegistry.class);
        ReactiveRepository<DeviceInstanceEntity, String> deviceRepository = Mockito.mock(ReactiveRepository.class);
        ReactiveRepository<DeviceProductEntity, String> productRepository = Mockito.mock(ReactiveRepository.class);

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setState((byte)1);

        Mockito.when(parent.getProduct(Mockito.anyString()))
            .thenReturn(InMemoryDeviceRegistry.create().register(deviceProductEntity.toProductInfo()));

        AutoDiscoverDeviceRegistry service = new AutoDiscoverDeviceRegistry(parent, deviceRepository, productRepository);
        service.getProduct(PRODUCT_ID)
            .map(DeviceProductOperator::getId)
            .as(StepVerifier::create)
            .expectNext(PRODUCT_ID)
            .verifyComplete();

        Mockito.when(parent.getProduct(Mockito.anyString()))
            .thenReturn(Mono.empty());
        Mockito.when(productRepository.findById(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductEntity));
        Mockito.when(parent.register(Mockito.any(ProductInfo.class)))
            .thenReturn(InMemoryDeviceRegistry.create().register(deviceProductEntity.toProductInfo()));
        service.getProduct(PRODUCT_ID)
            .map(DeviceProductOperator::getId)
            .as(StepVerifier::create)
            .expectNext(PRODUCT_ID)
            .verifyComplete();

        service.getProduct("")
            .map(DeviceProductOperator::getId)
            .switchIfEmpty(Mono.error(new NotFoundException()))
            .onErrorResume(e->Mono.just("产品id为空"))
            .as(StepVerifier::create)
            .expectNext("产品id为空")
            .verifyComplete();


    }

    @Test
    void register() {
        DeviceRegistry parent = Mockito.mock(DeviceRegistry.class);
        ReactiveRepository<DeviceInstanceEntity, String> deviceRepository = Mockito.mock(ReactiveRepository.class);
        ReactiveRepository<DeviceProductEntity, String> productRepository = Mockito.mock(ReactiveRepository.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        Mockito.when(parent.register(Mockito.any(DeviceInfo.class)))
            .thenReturn(InMemoryDeviceRegistry.create().register(deviceInstanceEntity.toDeviceInfo()));

        AutoDiscoverDeviceRegistry service = new AutoDiscoverDeviceRegistry(parent, deviceRepository, productRepository);
        service.register(deviceInstanceEntity.toDeviceInfo())
            .map(DeviceOperator::getDeviceId)
            .as(StepVerifier::create)
            .expectNext(DEVICE_ID)
            .verifyComplete();
    }

    @Test
    void testRegister() {
        DeviceRegistry parent = Mockito.mock(DeviceRegistry.class);
        ReactiveRepository<DeviceInstanceEntity, String> deviceRepository = Mockito.mock(ReactiveRepository.class);
        ReactiveRepository<DeviceProductEntity, String> productRepository = Mockito.mock(ReactiveRepository.class);
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        Mockito.when(parent.register(Mockito.any(ProductInfo.class)))
            .thenReturn(InMemoryDeviceRegistry.create().register(deviceProductEntity.toProductInfo()));

        AutoDiscoverDeviceRegistry service = new AutoDiscoverDeviceRegistry(parent, deviceRepository, productRepository);
        service.register(deviceProductEntity.toProductInfo())
            .map(DeviceProductOperator::getId)
            .as(StepVerifier::create)
            .expectNext(PRODUCT_ID)
            .verifyComplete();
    }

    @Test
    void unregisterDevice() {
        DeviceRegistry parent = Mockito.mock(DeviceRegistry.class);
        ReactiveRepository<DeviceInstanceEntity, String> deviceRepository = Mockito.mock(ReactiveRepository.class);
        ReactiveRepository<DeviceProductEntity, String> productRepository = Mockito.mock(ReactiveRepository.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);

        //先注册
        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).subscribe();
        Mockito.when(parent.unregisterDevice(Mockito.anyString()))
            .thenReturn(inMemoryDeviceRegistry.unregisterDevice(DEVICE_ID));//再注销

        AutoDiscoverDeviceRegistry service = new AutoDiscoverDeviceRegistry(parent, deviceRepository, productRepository);
        service.unregisterDevice(DEVICE_ID)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void unregisterProduct() {
        DeviceRegistry parent = Mockito.mock(DeviceRegistry.class);
        ReactiveRepository<DeviceInstanceEntity, String> deviceRepository = Mockito.mock(ReactiveRepository.class);
        ReactiveRepository<DeviceProductEntity, String> productRepository = Mockito.mock(ReactiveRepository.class);
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        //先注册
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        Mockito.when(parent.unregisterProduct(Mockito.anyString()))
            .thenReturn(inMemoryDeviceRegistry.unregisterProduct(PRODUCT_ID));//再注销
        AutoDiscoverDeviceRegistry service = new AutoDiscoverDeviceRegistry(parent, deviceRepository, productRepository);
        service.unregisterProduct(PRODUCT_ID)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }
}