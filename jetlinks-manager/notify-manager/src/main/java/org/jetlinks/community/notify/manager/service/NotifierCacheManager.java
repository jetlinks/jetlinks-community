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
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.template.TemplateManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class NotifierCacheManager {

    private final TemplateManager templateManager;

    private final NotifierManager notifierManager;

    public NotifierCacheManager(TemplateManager templateManager, NotifierManager notifierManager) {
        this.templateManager = templateManager;
        this.notifierManager = notifierManager;
    }

    @EventListener
    public void handleTemplateSave(EntitySavedEvent<NotifyTemplateEntity> event) {
        event.async(
            reloadTemplate(event.getEntity())
        );
    }

    @EventListener
    public void handleTemplateModify(EntityModifyEvent<NotifyTemplateEntity> event) {
        event.async(
            reloadTemplate(event.getBefore())
        );
    }

    @EventListener
    public void handleTemplateDelete(EntityDeletedEvent<NotifyTemplateEntity> event) {
        event.async(
            reloadTemplate(event.getEntity())
        );
    }
    @EventListener
    public void handleConfigSave(EntitySavedEvent<NotifyConfigEntity> event) {
        event.async(
            reloadConfig(event.getEntity())
        );
    }

    @EventListener
    public void handleConfigModify(EntityModifyEvent<NotifyConfigEntity> event) {
        event.async(
            reloadConfig(event.getBefore())
        );
    }

    @EventListener
    public void handleConfigDelete(EntityDeletedEvent<NotifyConfigEntity> event) {
        event.async(
            reloadConfig(event.getEntity())
        );
    }

    protected Mono<Void> reloadConfig(List<NotifyConfigEntity> entities) {
        return Flux.fromIterable(entities)
            .map(NotifyConfigEntity::getId)
            .doOnNext(id -> log.info("clear notifier config [{}] cache", id))
            .flatMap(notifierManager::reload)
            .then();
    }

    protected Mono<Void> reloadTemplate(List<NotifyTemplateEntity> entities) {
        return Flux.fromIterable(entities)
            .map(NotifyTemplateEntity::getId)
            .doOnNext(id -> log.info("clear template [{}] cache", id))
            .flatMap(templateManager::reload)
            .then();
    }

}
