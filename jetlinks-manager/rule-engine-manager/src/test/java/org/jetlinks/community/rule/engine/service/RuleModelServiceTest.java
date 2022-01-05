package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.core.MethodReferenceColumn;
import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.community.rule.engine.entity.RuleModelEntity;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RuleModelServiceTest {
    private static final String ID = "test";
    private final RuleInstanceService instanceServiceMock = Mockito.mock(RuleInstanceService.class);
    private final RuleEngineModelParser parserMock = Mockito.mock(RuleEngineModelParser.class);
    private final ReactiveRepository<RuleModelEntity, String> repository = Mockito.mock(ReactiveRepository.class);
    @Test
    RuleModelService getService(){
        Class<? extends RuleModelService> cls= RuleModelService.class;
        Constructor<? extends RuleModelService> constructor = null;
        RuleModelService service = null;
        try {
            constructor = cls.getConstructor();
            service = constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            try {
                if(field.getName().contains("instanceService")){
                    field.setAccessible(true);
                    field.set(service,instanceServiceMock);
                }
                if(field.getName().contains("modelParser")){
                    field.setAccessible(true);
                    field.set(service,parserMock);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        try {
            Field repository = cls.getSuperclass().getDeclaredField("repository");
            repository.setAccessible(true);
            repository.set(service,this.repository);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return service;
    }
    @Test
    void deploy() {
        RuleModelService service = getService();
        assertNotNull(service);
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
        RuleModelService service = getService();
        assertNotNull(service);
        ReactiveUpdate<RuleModelEntity> update = Mockito.mock(ReactiveUpdate.class);

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
        RuleModelService service = getService();
        assertNotNull(service);
        ReactiveUpdate<RuleModelEntity> update = Mockito.mock(ReactiveUpdate.class);

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
        Class<? extends RuleModelService> cls= RuleModelService.class;
        DefaultReactiveRepository reactiveRepository= new DefaultReactiveRepository(Mockito.mock(DatabaseOperator.class),"table", String.class,Mockito.mock(ResultWrapper.class)){
            @Override
            public Mono<Integer> insert(Publisher data) {
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
                return Mono.just(1);
            }
        };
        Constructor<? extends RuleModelService> constructor = null;
        RuleModelService service = null;
        try {
            constructor = cls.getConstructor();
            service = constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Field rep = cls.getSuperclass().getDeclaredField("repository");
            rep.setAccessible(true);
            rep.set(service,reactiveRepository);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(service);

        RuleModelEntity entity = new RuleModelEntity();
        entity.setVersion(null);
        service.insert(Mono.just(entity))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        DefaultReactiveRepository reactiveRepository1= new DefaultReactiveRepository(Mockito.mock(DatabaseOperator.class),"table", String.class,Mockito.mock(ResultWrapper.class)){
            @Override
            public Mono<Integer> insert(Publisher data) {
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
                return Mono.error(new IllegalArgumentException());
            }
        };

        try {
            Field rep = cls.getSuperclass().getDeclaredField("repository");
            rep.setAccessible(true);
            rep.set(service,reactiveRepository1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(service);
        service.insert(Mono.just(entity))
            .as(StepVerifier::create)
            .expectError()
            .verify();
    }
}