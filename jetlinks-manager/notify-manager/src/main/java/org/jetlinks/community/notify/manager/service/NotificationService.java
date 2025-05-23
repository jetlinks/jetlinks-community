/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.buffer.BufferSettings;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.manager.configuration.NotificationProperties;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotificationEntity;
import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.core.utils.Reactors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService extends GenericReactiveCrudService<NotificationEntity, String> implements CommandLineRunner {

    private final PersistenceBuffer<NotificationEntity> writer;

    public NotificationService(NotificationProperties properties) {
        writer = new PersistenceBuffer<>(
            BufferSettings.create(properties.getBuffer()),
            NotificationEntity::new,
            flux -> this.save(flux).then(Reactors.ALWAYS_FALSE))
            .retryWhenError(err -> ErrorUtils.hasException(
                err,
                CannotCreateTransactionException.class,
                NonTransientDataAccessException.class,
                TimeoutException.class,
                IOException.class))
            .name("notification");
        writer.init();
    }

    @PostConstruct
    public void init() {

    }

    @PreDestroy
    public void dispose() {
        writer.stop();
    }

    @Subscribe("/notifications/**")
    public Mono<Void> subscribeNotifications(Notification notification) {
        return writer.writeAsync(NotificationEntity.from(notification));
    }


    public Flux<NotificationEntity> findAndMarkRead(QueryParamEntity query) {
        return this
            .query(query)
            .buffer(200)
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> this
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
                         .doOnNext(e -> e.setState(NotificationState.read)),
                     8,
                     8);
    }

    @Override
    public void run(String... args) throws Exception {
        writer.start();
        SpringApplication
            .getShutdownHandlers()
            .add(writer::dispose);
    }
}
