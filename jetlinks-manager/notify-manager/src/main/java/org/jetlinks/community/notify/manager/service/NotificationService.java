package org.jetlinks.community.notify.manager.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.buffer.BufferProperties;
import org.jetlinks.community.buffer.BufferSettings;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotificationEntity;
import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.jetlinks.community.utils.ErrorUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConfigurationProperties(prefix = "jetlinks.notification")
public class NotificationService extends GenericReactiveCrudService<NotificationEntity, String> {

    @Getter
    @Setter
    private BufferProperties buffer = new BufferProperties();

    private PersistenceBuffer<NotificationEntity> writer;

    public NotificationService() {
        buffer.setFilePath("./data/notification-buffer");
        buffer.setSize(1000);
        buffer.setTimeout(Duration.ofSeconds(1));
    }

    @PostConstruct
    public void init() {

        writer = new PersistenceBuffer<>(
            BufferSettings.create(buffer),
            NotificationEntity::new,
            flux -> this.save(flux).then(Reactors.ALWAYS_FALSE))
            .retryWhenError(err -> ErrorUtils.hasException(
                err,
                CannotCreateTransactionException.class,
                NonTransientDataAccessException.class,
                TimeoutException.class,
                IOException.class))
            .name("notification");

        writer.start();

    }

    @PreDestroy
    public void dispose() {
        writer.dispose();
    }

    @Subscribe("/notifications/**")
    public Mono<Void> subscribeNotifications(Notification notification) {
        writer.write(NotificationEntity.from(notification));
        return Mono.empty();
    }

    public Flux<NotificationEntity> findAndMarkRead(QueryParamEntity query) {
        return this
            .query(query)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMapMany(list -> this
                .createUpdate()
                .set(NotificationEntity::getState, NotificationState.read)
                .where()
                .in(NotificationEntity::getId, list
                    .stream()
                    .map(NotificationEntity::getId)
                    .collect(Collectors.toList()))
                .and(NotificationEntity::getState, NotificationState.unread)
                .execute()
                .thenMany(Flux.fromIterable(list))
                .doOnNext(e -> e.setState(NotificationState.read)));
    }

}
