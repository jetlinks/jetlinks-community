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

import java.util.Collection;

class LocalDeviceProductServiceTest {
    public static final String ID_1 = "test001";

    @Test
    void deploy() {
        ReactiveRepository<DeviceProductEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<DeviceProductEntity> update = Mockito.mock(ReactiveUpdate.class);
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(ID_1);
        deviceProductEntity.setName("test");
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setMetadata("test");


        Mockito.when(repository.findById(Mockito.any(Mono.class))).thenReturn(Mono.justOrEmpty(deviceProductEntity));

        //模拟设备注册
        DeviceRegistry deviceRegistry = Mockito.mock(DeviceRegistry.class);

        Mockito.when(deviceRegistry.register(Mockito.any(ProductInfo.class))).thenReturn(new InMemoryDeviceRegistry().register(deviceProductEntity.toProductInfo()));


        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));


        ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        ReactiveRepository<DeviceInstanceEntity, String> reactiveRepository = Mockito.mock(ReactiveRepository.class);

        LocalDeviceProductService service = new LocalDeviceProductService(deviceRegistry, applicationEventPublisher, reactiveRepository) {
            @Override
            public ReactiveRepository<DeviceProductEntity, String> getRepository() {
                return repository;
            }
        };

//      service.deploy(ID_1).subscribe(System.out::println);
        Mono<Integer> deploy = service.deploy(ID_1);
        StepVerifier.create(deploy)
            .expectNext(1)
            .expectComplete()
            .verify();

    }

    @Test
    void cancelDeploy() {
        ReactiveRepository<DeviceProductEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<DeviceProductEntity> update = Mockito.mock(ReactiveUpdate.class);

        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        DeviceRegistry deviceRegistry = Mockito.mock(DeviceRegistry.class);
        ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        ReactiveRepository<DeviceInstanceEntity, String> reactiveRepository = Mockito.mock(ReactiveRepository.class);


        LocalDeviceProductService service = new LocalDeviceProductService(deviceRegistry, applicationEventPublisher, reactiveRepository) {
            @Override
            public ReactiveRepository<DeviceProductEntity, String> getRepository() {
                return repository;
            }
        };
        service.cancelDeploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

//    @Test
//    void deleteById1(){
//        ReactiveRepository<DeviceProductEntity, String> repository = Mockito.mock(ReactiveRepository.class);
//        DeviceRegistry deviceRegistry = Mockito.mock(DeviceRegistry.class);
//
//        ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
//        ReactiveRepository<DeviceInstanceEntity, String> reactiveRepository = Mockito.mock(ReactiveRepository.class);
//        ReactiveQuery<DeviceInstanceEntity> query = Mockito.mock(ReactiveQuery.class);
//
//
//        Mockito.when(reactiveRepository.createQuery()).thenReturn(query);
//        Mockito.when(query.where()).thenReturn(query);
//        Mockito.when(query.in(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Collection.class))).thenReturn(query);
//        Mockito.when(query.count()).thenReturn(Mono.just(1));
//
//
//        LocalDeviceProductService service = new LocalDeviceProductService(deviceRegistry, applicationEventPublisher, reactiveRepository) {
//            @Override
//            public ReactiveRepository<DeviceProductEntity, String> getRepository() {
//                return repository;
//            }
//        };
//        service.deleteById(Mono.just(ID_1))
//            .map(Object::toString)
//            .onErrorResume(e->Mono.just(e.getMessage()))
//            .as(StepVerifier::create)
//            .expectNext("存在关联设备,无法删除!")
//            .verifyComplete();
//    }


    @Test
    void deleteById() {

        ReactiveRepository<DeviceProductEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry deviceRegistry = Mockito.mock(DeviceRegistry.class);

        ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        ReactiveRepository<DeviceInstanceEntity, String> reactiveRepository = Mockito.mock(ReactiveRepository.class);
        ReactiveQuery<DeviceInstanceEntity> query = Mockito.mock(ReactiveQuery.class);


        Mockito.when(reactiveRepository.createQuery()).thenReturn(query);
        Mockito.when(query.where()).thenReturn(query);
        Mockito.when(query.in(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Collection.class))).thenReturn(query);
        Mockito.when(query.count()).thenReturn(Mono.just(-1));

        Mockito.when(repository.deleteById(Mockito.any(Publisher.class))).thenReturn(Mono.just(1));

        LocalDeviceProductService service = new LocalDeviceProductService(deviceRegistry, applicationEventPublisher, reactiveRepository) {
            @Override
            public ReactiveRepository<DeviceProductEntity, String> getRepository() {
                return repository;
            }
        };
        service.deleteById(Mono.just(ID_1))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        Mockito.when(query.count()).thenReturn(Mono.just(1));
        service.deleteById(Mono.just(ID_1))
            .map(Object::toString)
            .onErrorResume(e->Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("存在关联设备,无法删除!")
            .verifyComplete();

    }

}