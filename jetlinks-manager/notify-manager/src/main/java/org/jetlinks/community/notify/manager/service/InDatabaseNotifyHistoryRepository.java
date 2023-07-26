package org.jetlinks.community.notify.manager.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.jetlinks.community.notify.manager.entity.NotifyHistoryEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class InDatabaseNotifyHistoryRepository implements NotifyHistoryRepository {

    private final NotifyHistoryService historyService;

    @Subscribe("/notify/**")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> handleNotify(SerializableNotifierEvent event) {

        return historyService
            .insert(Mono.just(NotifyHistoryEntity.of(event)))
            .then();
    }

    @Override
    public Mono<PagerResult<NotifyHistory>> queryPager(QueryParamEntity param) {
        return historyService
            .queryPager(param, NotifyHistoryEntity::toHistory);
    }
}
