package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.core.MethodReferenceColumn;
import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.task.Task;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;

class RuleInstanceServiceTest {
    private static final String ID = "test";
    private final RuleEngine ruleEngine = Mockito.mock(RuleEngine.class);
    private final RuleEngineModelParser modelParser = Mockito.mock(RuleEngineModelParser.class);
    private final ElasticSearchService elasticSearchService = Mockito.mock(ElasticSearchService.class);
    private final ReactiveRepository<RuleInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);

    RuleInstanceService getService() {
        Class<? extends RuleInstanceService> cls = RuleInstanceService.class;
        Constructor constructor = null;
        try {
            constructor = cls.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        constructor.setAccessible(true);
        RuleInstanceService service = null;
        try {
            service = (RuleInstanceService) constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Field[] fields = cls.getDeclaredFields();

        for (Field field : fields) {
            try {
                if (field.getName().contains("ruleEngine")) {
                    field.setAccessible(true);
                    field.set(service, ruleEngine);
                }
                if (field.getName().contains("modelParser")) {
                    field.setAccessible(true);
                    field.set(service, modelParser);
                }
                if (field.getName().contains("elasticSearchService")) {
                    field.setAccessible(true);
                    field.set(service, elasticSearchService);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        Field[] declaredFields = cls.getSuperclass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            try {
                declaredField.setAccessible(true);
                declaredField.set(service, repository);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return service;
    }

    @Test
    void queryExecuteEvent() {
        RuleInstanceService service = getService();

        Mockito.when(elasticSearchService.queryPager(Mockito.any(ElasticIndex.class),Mockito.any(QueryParam.class),Mockito.any(Class.class)))
            .thenReturn(Mono.just(PagerResult.of(1,new ArrayList<>())));

        service.queryExecuteEvent(new QueryParam())
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

    }

    @Test
    void queryExecuteLog() {
        RuleInstanceService service = getService();

        Mockito.when(elasticSearchService.queryPager(Mockito.any(ElasticIndex.class),Mockito.any(QueryParam.class),Mockito.any(Class.class)))
            .thenReturn(Mono.just(PagerResult.of(1,new ArrayList<>())));

        service.queryExecuteLog(new QueryParam())
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void stop() {
        ReactiveUpdate<RuleInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        RuleInstanceService service = getService();
        Mockito.when(ruleEngine.shutdown(Mockito.anyString()))
            .thenReturn(Mono.just(1).then());
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
    void start() {
        RuleInstanceService service = getService();
        RuleInstanceEntity entity = new RuleInstanceEntity();
        entity.setId(ID);
        entity.setModelId("modelId");
        entity.setName("test");
        entity.setModelType("ss");
        entity.setModelMeta("TEST");
        Mockito.when(repository.findById(Mockito.any(Mono.class)))
            .thenReturn(Mono.just(entity));

        Mockito.when(modelParser.parse(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(new RuleModel());
        Mockito.when(ruleEngine.startRule(Mockito.anyString(), Mockito.any(RuleModel.class)))
            .thenReturn(Flux.just(Mockito.mock(Task.class)));
        ReactiveUpdate<RuleInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate())
            .thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(MethodReferenceColumn.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        service.start(ID)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void deleteById() {
        ReactiveUpdate<RuleInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        DefaultReactiveRepository<RuleInstanceEntity, String> reactiveRepository= new DefaultReactiveRepository(Mockito.mock(DatabaseOperator.class),"table", String.class,Mockito.mock(ResultWrapper.class)){
            @Override
            public Mono<Integer> deleteById(Publisher key) {
                key.subscribe(new Subscriber() {
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

            @Override
            public ReactiveUpdate createUpdate() {
                return update;
            }
        };


        Class<? extends RuleInstanceService> cls = RuleInstanceService.class;
        Constructor constructor = null;
        try {
            constructor = cls.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        RuleInstanceService service = null;
        try {
            service = (RuleInstanceService) constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Field[] fields = cls.getDeclaredFields();

        for (Field field : fields) {
            try {
                if (field.getName().contains("ruleEngine")) {
                    field.setAccessible(true);
                    field.set(service, ruleEngine);
                }
                if (field.getName().contains("modelParser")) {
                    field.setAccessible(true);
                    field.set(service, modelParser);
                }
                if (field.getName().contains("elasticSearchService")) {
                    field.setAccessible(true);
                    field.set(service, elasticSearchService);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        Field[] declaredFields = cls.getSuperclass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            try {
                declaredField.setAccessible(true);
                declaredField.set(service, reactiveRepository);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        Mockito.when(ruleEngine.shutdown(Mockito.anyString()))
            .thenReturn(Mono.just(1).then());
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        service.deleteById(Mono.just(ID))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void run(){
        RuleInstanceService service = getService();
        ReactiveQuery<RuleInstanceEntity> update = Mockito.mock(ReactiveQuery.class);
        Mockito.when(repository.createQuery())
            .thenReturn(update);
        Mockito.when(update.where())
            .thenReturn(update);
        Mockito.when(update.is(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        RuleInstanceEntity entity = new RuleInstanceEntity();
        entity.setId(ID);
        entity.setModelId("modelId");
        entity.setName("test");
        entity.setModelType("ss");
        entity.setModelMeta("TEST");
        Mockito.when(update.fetch())
            .thenReturn(Flux.just(entity));

        Mockito.when(modelParser.parse(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(new RuleModel());
        Mockito.when(ruleEngine.startRule(Mockito.anyString(), Mockito.any(RuleModel.class)))
            .thenReturn(Flux.error(IllegalAccessError::new));
        service.run();
    }
}