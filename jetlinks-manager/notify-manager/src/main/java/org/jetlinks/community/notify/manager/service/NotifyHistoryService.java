package org.jetlinks.community.notify.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.jetlinks.community.notify.manager.entity.NotifyHistoryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class NotifyHistoryService extends GenericReactiveCrudService<NotifyHistoryEntity, String> {


    @Subscribe("/notify/**")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> handleNotify(SerializableNotifierEvent event) {
        return insert(Mono.just(NotifyHistoryEntity.of(event))).then();
    }

}
