package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.core.MethodReferenceColumn;
import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.community.rule.engine.device.DeviceAlarmRule;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceAlarmServiceTest {
    private static final String ID = "test";

    @Test
    void save() {
        RuleInstanceService instanceService = Mockito.mock(RuleInstanceService.class);

        DefaultReactiveRepository reactiveRepository= new DefaultReactiveRepository(Mockito.mock(DatabaseOperator.class),"table", String.class,Mockito.mock(ResultWrapper.class)){
            @Override
            public Mono<SaveResult> save(Publisher data) {
                data.subscribe(new Subscriber() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1L);
                    }

                    @Override
                    public void onNext(Object o) {

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
        };
        DeviceAlarmService service = new DeviceAlarmService(instanceService){
            @Override
            public ReactiveRepository<DeviceAlarmEntity, String> getRepository() {
                return reactiveRepository;
            }
        };

        assertNotNull(service);
        Mockito.when(instanceService.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(SaveResult.of(1,0)));


        DeviceAlarmEntity entity = new DeviceAlarmEntity();
        entity.setAlarmRule(new DeviceAlarmRule());
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
    void start() {
        RuleInstanceService instanceService = Mockito.mock(RuleInstanceService.class);
        ReactiveRepository<DeviceAlarmEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceAlarmService service = new DeviceAlarmService(instanceService){
            @Override
            public ReactiveRepository<DeviceAlarmEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);

        DeviceAlarmEntity alarmEntity = new DeviceAlarmEntity();
        alarmEntity.setId(ID);
        DeviceAlarmRule alarmRule = new DeviceAlarmRule();
        alarmEntity.setAlarmRule(alarmRule);
        Mockito.when(repository.findById(Mockito.anyString()))
            .thenReturn(Mono.just(alarmEntity));
        Mockito.when(instanceService.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(SaveResult.of(1,0)));
        Mockito.when(instanceService.start(Mockito.anyString()))
            .thenReturn(Mono.just(1).then());

        ReactiveUpdate<DeviceAlarmEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate())
            .thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(MethodReferenceColumn.class)))
            .thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        service.start(ID)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void stop() {
        RuleInstanceService instanceService = Mockito.mock(RuleInstanceService.class);
        ReactiveRepository<DeviceAlarmEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceAlarmService service = new DeviceAlarmService(instanceService){
            @Override
            public ReactiveRepository<DeviceAlarmEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        Mockito.when(instanceService.stop(Mockito.anyString()))
            .thenReturn(Mono.just(1).then());
        ReactiveUpdate<DeviceAlarmEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate())
            .thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));
        service.stop(ID)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void deleteById() {
        RuleInstanceService instanceService = Mockito.mock(RuleInstanceService.class);
        ReactiveRepository<DeviceAlarmEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceAlarmService service = new DeviceAlarmService(instanceService){
            @Override
            public ReactiveRepository<DeviceAlarmEntity, String> getRepository() {
                return repository;
            }
        };

        assertNotNull(service);
        Mockito.when(instanceService.stop(Mockito.anyString()))
            .thenReturn(Mono.just(1).then());
        Mockito.when(instanceService.deleteById(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(repository.deleteById(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));

        service.deleteById(Mono.just(ID))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

    }
}