package org.jetlinks.community.visualization.service;

import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.community.visualization.entity.DataVisualizationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DataVisualizationServiceTest {

    @Test
    void save() {
        DefaultReactiveRepository repository =
            new DefaultReactiveRepository(Mockito.mock(DatabaseOperator.class),"table", Class.class,Mockito.mock(ResultWrapper.class)){
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
        DataVisualizationService service = new DataVisualizationService(){
            @Override
            public ReactiveRepository<DataVisualizationEntity, String> getRepository() {
                return repository;
            }
        };
        DataVisualizationEntity entity = new DataVisualizationEntity();
        entity.setId("test");
        entity.setType("test");
        entity.setTarget("test");
        service.save(Mono.just(entity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }
}