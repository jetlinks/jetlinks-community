package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceAlarmHistoryServiceTest {

    @Test
    void init() {
        ReactiveRepository<DeviceAlarmHistoryEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceAlarmHistoryService service = new DeviceAlarmHistoryService() {
            @Override
            public ReactiveRepository<DeviceAlarmHistoryEntity, String> getRepository() {
                return repository;
            }
        };
        Mockito.when(repository.insertBatch(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
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
        map.put("timestamp",System.currentTimeMillis());

        service.saveAlarm(map)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }
}