package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceAlarmHistoryServiceTest {

    @Test
    void init() {
        ReactiveRepository<DeviceAlarmHistoryEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceAlarmHistoryService service = new DeviceAlarmHistoryService() {
            @Override
            public Mono<Integer> insertBatch(Publisher<? extends Collection<DeviceAlarmHistoryEntity>> entityPublisher) {
                entityPublisher.subscribe(new Subscriber<Collection<DeviceAlarmHistoryEntity>>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1L);
                    }

                    @Override
                    public void onNext(Collection<DeviceAlarmHistoryEntity> deviceAlarmHistoryEntities) {

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
//        Mockito.when(repository.insertBatch(Mockito.any(Publisher.class)))
//            .thenReturn(Mono.just(1));
        assertNotNull(service);
        Map<String, Object> map = new HashMap<>();
        map.put("productId","test");
        map.put("timestamp", System.currentTimeMillis());
        service.saveAlarm(map).subscribe();
        service.init();
    }

    @Test
    void saveAlarm() {
        ReactiveRepository<DeviceAlarmHistoryEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceAlarmHistoryService service = new DeviceAlarmHistoryService() {
            @Override
            public ReactiveRepository<DeviceAlarmHistoryEntity, String> getRepository() {
                return repository;
            }
        };
        Map<String, Object> map = new HashMap<>();
        map.put("productId","test");
        map.put("timestamp", System.currentTimeMillis());

        assertNotNull(service);
        service.saveAlarm(map)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }
}