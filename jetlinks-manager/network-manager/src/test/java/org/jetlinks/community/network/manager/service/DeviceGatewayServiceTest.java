package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.reactivestreams.Publisher;



import static org.junit.jupiter.api.Assertions.*;

class DeviceGatewayServiceTest {
    private final static String ID = "test";

    @Test
    void updateState() {
        ReactiveRepository<DeviceGatewayEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<DeviceGatewayEntity> update = Mockito.mock(ReactiveUpdate.class);


        Mockito.when(repository.createUpdate())
            .thenReturn(update);
        Mockito.when(update.where())
            .thenReturn(update);
        Mockito.when(update.and(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);

        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        DeviceGatewayService service = new DeviceGatewayService() {
            @Override
            public ReactiveRepository<DeviceGatewayEntity, String> getRepository() {
                return repository;
            }
        };
        service.updateState(ID, NetworkConfigState.enabled)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

    }

    @Test
    void save() {

        class TestDefaultReactiveRepository extends DefaultReactiveRepository<DeviceGatewayEntity, String>{

            public TestDefaultReactiveRepository(DatabaseOperator operator, String table, Class<DeviceGatewayEntity> type, ResultWrapper<DeviceGatewayEntity, ?> wrapper) {
                super(operator, table, type, wrapper);
            }
            @Override
            public Mono<SaveResult> save(Publisher<DeviceGatewayEntity> data) {
                data.subscribe(new Subscriber<DeviceGatewayEntity>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(DeviceGatewayEntity deviceGatewayEntity) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
                return Mono.just(SaveResult.of(1,0));
            }
        }
        DatabaseOperator operator = Mockito.mock(DatabaseOperator.class);
        ResultWrapper<DeviceGatewayEntity, ?> resultWrapper = Mockito.mock(ResultWrapper.class);

        TestDefaultReactiveRepository testDefaultReactiveRepository = new TestDefaultReactiveRepository(operator,"",DeviceGatewayEntity.class,resultWrapper);


        DeviceGatewayService service = new DeviceGatewayService() {
            @Override
            public ReactiveRepository<DeviceGatewayEntity, String> getRepository() {
                return testDefaultReactiveRepository;
            }
        };

        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        service.save(Mono.just(entity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
        entity.setId(ID);
        service.save(Mono.just(entity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void insert() {
        ReactiveRepository<DeviceGatewayEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        Mockito.when(repository.insert(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));

        DeviceGatewayService service = new DeviceGatewayService() {
            @Override
            public ReactiveRepository<DeviceGatewayEntity, String> getRepository() {
                return repository;
            }
        };
        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        service.insert(Mono.just(entity))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void deleteById() {
        ReactiveRepository<DeviceGatewayEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setId(ID);
        entity.setState(NetworkConfigState.disabled);
        Mockito.when(repository.findById(Mockito.any(Mono.class)))
            .thenReturn(Mono.just(entity));

        DeviceGatewayService service = new DeviceGatewayService() {
            @Override
            public ReactiveRepository<DeviceGatewayEntity, String> getRepository() {
                return repository;
            }
        };
        Mockito.when(repository.deleteById(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        service.deleteById(Mono.just(ID))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        entity.setState(NetworkConfigState.enabled);
        Mockito.when(repository.findById(Mockito.any(Mono.class)))
            .thenReturn(Mono.just(entity));

        service.deleteById(Mono.just(ID))
            .as(StepVerifier::create)
            .expectError(UnsupportedOperationException.class)
            .verify();

        Mockito.when(repository.findById(Mockito.any(Mono.class)))
            .thenReturn(Mono.empty());
        service.deleteById(Mono.just(ID))
            .as(StepVerifier::create)
            .expectError(NotFoundException.class)
            .verify();
    }
}