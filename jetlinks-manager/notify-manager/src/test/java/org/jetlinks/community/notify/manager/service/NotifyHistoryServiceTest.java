package org.jetlinks.community.notify.manager.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.jetlinks.community.notify.manager.entity.NotifyHistoryEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifyHistoryServiceTest {

    @Test
    void handleNotify() {
        ReactiveRepository<NotifyHistoryEntity, String> repository = Mockito.mock(ReactiveRepository.class);

        Mockito.when(repository.insert(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));

        NotifyHistoryService service = new NotifyHistoryService() {
            @Override
            public ReactiveRepository<NotifyHistoryEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        SerializableNotifierEvent serializableNotifierEvent = new SerializableNotifierEvent();
        serializableNotifierEvent.setSuccess(true);
        serializableNotifierEvent.setCause("test");
        serializableNotifierEvent.setNotifierId("test");
        serializableNotifierEvent.setNotifyType("test");
        serializableNotifierEvent.setProvider("test");
        serializableNotifierEvent.setContext(new HashMap<>());
        service.handleNotify(serializableNotifierEvent)
            .onErrorResume(s-> Mono.error(new RuntimeException()))
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }

}