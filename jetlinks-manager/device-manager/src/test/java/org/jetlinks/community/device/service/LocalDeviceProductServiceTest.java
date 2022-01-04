package org.jetlinks.community.device.service;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalDeviceProductServiceTest {
    public static final String ID_1 = "test001";
    private final ReactiveRepository<DeviceProductEntity, String> repository = Mockito.mock(ReactiveRepository.class);
    private final ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
    private final ReactiveRepository<DeviceInstanceEntity, String> reactiveRepository = Mockito.mock(ReactiveRepository.class);
    //模拟设备注册
    private final DeviceRegistry deviceRegistry = Mockito.mock(DeviceRegistry.class);

    LocalDeviceProductService getService() {
        Class<? extends LocalDeviceProductService> serviceClass = LocalDeviceProductService.class;
        LocalDeviceProductService service = null;
        try {
            Constructor<? extends LocalDeviceProductService> constructor = serviceClass.getConstructor();
            service = constructor.newInstance();
            Field registry = serviceClass.getDeclaredField("registry");
            registry.setAccessible(true);
            registry.set(service,this.deviceRegistry);
            Field eventPublisher = serviceClass.getDeclaredField("eventPublisher");
            eventPublisher.setAccessible(true);
            eventPublisher.set(service,this.applicationEventPublisher);
            Field instanceRepository = serviceClass.getDeclaredField("instanceRepository");
            instanceRepository.setAccessible(true);
            instanceRepository.set(service,this.reactiveRepository);
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            Field repository = serviceClass.getSuperclass().getDeclaredField("repository");
            repository.setAccessible(true);
            repository.set(service,this.repository);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return service;
    }

    @Test
    void deploy() {

        ReactiveUpdate<DeviceProductEntity> update = Mockito.mock(ReactiveUpdate.class);
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(ID_1);
        deviceProductEntity.setName("test");
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setMetadata("test");

        Mockito.when(repository.findById(Mockito.any(Mono.class))).thenReturn(Mono.justOrEmpty(deviceProductEntity));


        Mockito.when(deviceRegistry.register(Mockito.any(ProductInfo.class))).thenReturn(new InMemoryDeviceRegistry().register(deviceProductEntity.toProductInfo()));


        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));


        LocalDeviceProductService service = getService();
        assertNotNull(service);
        Mono<Integer> deploy = service.deploy(ID_1);
        StepVerifier.create(deploy)
            .expectNext(1)
            .expectComplete()
            .verify();

    }

    @Test
    void cancelDeploy() {
        ReactiveUpdate<DeviceProductEntity> update = Mockito.mock(ReactiveUpdate.class);

        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        LocalDeviceProductService service = getService();
        assertNotNull(service);
        service.cancelDeploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }


    @Test
    void deleteById() {

        ReactiveQuery<DeviceInstanceEntity> query = Mockito.mock(ReactiveQuery.class);


        Mockito.when(reactiveRepository.createQuery()).thenReturn(query);
        Mockito.when(query.where()).thenReturn(query);
        Mockito.when(query.in(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Collection.class))).thenReturn(query);
        Mockito.when(query.count()).thenReturn(Mono.just(-1));

        Mockito.when(repository.deleteById(Mockito.any(Publisher.class))).thenReturn(Mono.just(1));

        LocalDeviceProductService service = getService();
        assertNotNull(service);
        service.deleteById(Mono.just(ID_1))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        Mockito.when(query.count()).thenReturn(Mono.just(1));
        service.deleteById(Mono.just(ID_1))
            .map(Object::toString)
            .onErrorResume(e -> Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("存在关联设备,无法删除!")
            .verifyComplete();

    }

}