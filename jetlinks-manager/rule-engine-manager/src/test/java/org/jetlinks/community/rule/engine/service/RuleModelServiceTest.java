package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.core.MethodReferenceColumn;
import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.jetlinks.community.rule.engine.entity.RuleModelEntity;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.reactivestreams.Publisher;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RuleModelServiceTest {
    private static final String ID = "test";


    @Test
    void deploy() {
        RuleInstanceService instanceServiceMock = Mockito.mock(RuleInstanceService.class);
        RuleEngineModelParser parserMock = Mockito.mock(RuleEngineModelParser.class);
        ReactiveRepository<RuleModelEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        RuleModelService service = new RuleModelService(instanceServiceMock,parserMock){
            @Override
            public ReactiveRepository<RuleModelEntity, String> getRepository() {
                return repository;
            }
        };

        RuleModelEntity modelEntity = new RuleModelEntity();
        modelEntity.setId(ID);
        modelEntity.setModelType("test");
        modelEntity.setModelMeta("test");
        modelEntity.setDescription("test");
        modelEntity.setVersion(1);
        Mockito.when(repository.findById(Mockito.any(Mono.class)))
            .thenReturn(Mono.just(modelEntity));
        RuleModel ruleModel = new RuleModel();
        ruleModel.setId(ID);
        Mockito.when(parserMock.parse(Mockito.anyString(),Mockito.anyString()))
            .thenReturn(ruleModel);
        Mockito.when(instanceServiceMock.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(SaveResult.of(1,0)));

        service.deploy(ID)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

    }

    @Test
    void updateById() {
        RuleInstanceService instanceServiceMock = Mockito.mock(RuleInstanceService.class);
        RuleEngineModelParser parserMock = Mockito.mock(RuleEngineModelParser.class);
        ReactiveRepository<RuleModelEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<RuleModelEntity> update = Mockito.mock(ReactiveUpdate.class);
        RuleModelService service = new RuleModelService(instanceServiceMock, parserMock) {
            @Override
            public ReactiveRepository<RuleModelEntity, String> getRepository() {
                return repository;
            }
        };

        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(RuleModelEntity.class)))
            .thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(MethodReferenceColumn.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        RuleModelEntity entity = new RuleModelEntity();
        service.updateById(ID,Mono.just(entity))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void save() {
        RuleInstanceService instanceServiceMock = Mockito.mock(RuleInstanceService.class);
        RuleEngineModelParser parserMock = Mockito.mock(RuleEngineModelParser.class);
        ReactiveRepository<RuleModelEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<RuleModelEntity> update = Mockito.mock(ReactiveUpdate.class);
        RuleModelService service = new RuleModelService(instanceServiceMock, parserMock) {
            @Override
            public ReactiveRepository<RuleModelEntity, String> getRepository() {
                return repository;
            }
        };
        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(RuleModelEntity.class)))
            .thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(MethodReferenceColumn.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        RuleModelEntity entity = new RuleModelEntity();
        service.save(Mono.just(entity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        Mockito.when(update.execute())
            .thenReturn(Mono.just(0));
        Mockito.when(repository.insert(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        service.save(Mono.just(entity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void insert() {
        RuleInstanceService instanceServiceMock = Mockito.mock(RuleInstanceService.class);
        RuleEngineModelParser parserMock = Mockito.mock(RuleEngineModelParser.class);

        RuleModelService service = new RuleModelService(instanceServiceMock, parserMock) {

            @Override
            public Mono<Integer> insert(Publisher<RuleModelEntity> entityPublisher) {
                entityPublisher.subscribe(new Subscriber<RuleModelEntity>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1L);
                    }

                    @Override
                    public void onNext(RuleModelEntity ruleModelEntity) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
                return Mono.just(1);
            }
        };

        RuleModelEntity entity = new RuleModelEntity();
        entity.setVersion(null);
        service.insert(Mono.just(entity))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }
}