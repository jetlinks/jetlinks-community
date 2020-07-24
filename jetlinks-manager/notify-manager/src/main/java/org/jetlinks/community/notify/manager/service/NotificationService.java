package org.jetlinks.community.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotificationEntity;
import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.springframework.stereotype.Service;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService extends GenericReactiveCrudService<NotificationEntity, String> {


    private final EmitterProcessor<NotificationEntity> processor = EmitterProcessor.create();

    private final FluxSink<NotificationEntity> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    @PostConstruct
    public void init() {

        FluxUtils
            .bufferRate(processor, 1000, 200, Duration.ofSeconds(3))
            .flatMap(buffer -> this.save(Flux.fromIterable(buffer)))
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
            .subscribe()
        ;

    }

    @Subscribe("/notifications/**")
    public Mono<Void> subscribeNotifications(Notification notification) {
        return Mono.fromRunnable(() -> sink.next(NotificationEntity.from(notification)));
    }

    public Flux<NotificationEntity> findAndMarkRead(QueryParamEntity query) {
        return query(query)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMapMany(list -> createUpdate()
                .set(NotificationEntity::getState, NotificationState.read)
                .where()
                .in(NotificationEntity::getId, list.stream().map(NotificationEntity::getId).collect(Collectors.toList()))
                .and(NotificationEntity::getState, NotificationState.unread)
                .execute()
                .thenMany(Flux.fromIterable(list)
                    .doOnNext(e -> e.setState(NotificationState.read))));
    }

}
