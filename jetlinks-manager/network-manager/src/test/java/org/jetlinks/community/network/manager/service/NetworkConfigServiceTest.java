package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkProperties;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NetworkConfigServiceTest {
    private final static String ID = "test";

    @Test
    void getConfig() {
        ReactiveRepository<NetworkConfigEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        NetworkConfigService service = new NetworkConfigService() {
            @Override
            public ReactiveRepository<NetworkConfigEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        NetworkConfigEntity entity = new NetworkConfigEntity();
        entity.setId(ID);
        entity.setState(NetworkConfigState.enabled);
        Mockito.when(repository.findById(Mockito.anyString()))
            .thenReturn(Mono.just(entity));
        service.getConfig(DefaultNetworkType.TCP_SERVER, ID)
            .map(NetworkProperties::isEnabled)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void save() {

        class TestDefaultReactiveRepository extends DefaultReactiveRepository<NetworkConfigEntity, String> {

            public TestDefaultReactiveRepository(DatabaseOperator operator, String table, Class<NetworkConfigEntity> type, ResultWrapper<NetworkConfigEntity, ?> wrapper) {
                super(operator, table, type, wrapper);
            }
            @Override
            public Mono<SaveResult> save(Publisher<NetworkConfigEntity> data) {
                data.subscribe(new Subscriber<NetworkConfigEntity>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(NetworkConfigEntity networkConfigEntity) {
                        System.out.println(networkConfigEntity);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.getMessage();
                    }

                    @Override
                    public void onComplete() {
                        Mono.just(1);
                    }
                });
                return Mono.just(SaveResult.of(1,0));
            }
        }
        DatabaseOperator operator = Mockito.mock(DatabaseOperator.class);
        ResultWrapper<NetworkConfigEntity, ?> resultWrapper = Mockito.mock(ResultWrapper.class);

        TestDefaultReactiveRepository testDefaultReactiveRepository = new TestDefaultReactiveRepository(operator,"", NetworkConfigEntity.class,resultWrapper);


        NetworkConfigService service = new NetworkConfigService() {
            @Override
            public ReactiveRepository<NetworkConfigEntity, String> getRepository() {
                return testDefaultReactiveRepository;
            }
        };
        assertNotNull(service);
        NetworkConfigEntity entity = new NetworkConfigEntity();
        service.save(Mono.just(entity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
        entity.setId(ID);
        entity.setState(NetworkConfigState.enabled);
        service.save(Mono.just(entity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void insert() {
        class TestDefaultReactiveRepository extends DefaultReactiveRepository<NetworkConfigEntity, String> {

            public TestDefaultReactiveRepository(DatabaseOperator operator, String table, Class<NetworkConfigEntity> type, ResultWrapper<NetworkConfigEntity, ?> wrapper) {
                super(operator, table, type, wrapper);
            }

            @Override
            public Mono<Integer> insert(Publisher<NetworkConfigEntity> data) {
                data.subscribe(new Subscriber<NetworkConfigEntity>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(NetworkConfigEntity networkConfigEntity) {
                        System.out.println(networkConfigEntity);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.getMessage();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println(1);
                    }
                });
                return Mono.just(1);
            }

        }
        DatabaseOperator operator = Mockito.mock(DatabaseOperator.class);
        ResultWrapper<NetworkConfigEntity, ?> resultWrapper = Mockito.mock(ResultWrapper.class);

        TestDefaultReactiveRepository testDefaultReactiveRepository = new TestDefaultReactiveRepository(operator,"", NetworkConfigEntity.class,resultWrapper);


        NetworkConfigService service = new NetworkConfigService() {
            @Override
            public ReactiveRepository<NetworkConfigEntity, String> getRepository() {
                return testDefaultReactiveRepository;
            }
        };
        assertNotNull(service);
        NetworkConfigEntity entity = new NetworkConfigEntity();
        entity.setId(ID);
        entity.setState(NetworkConfigState.enabled);
        service.insert(Mono.just(entity))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }
}