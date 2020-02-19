package org.jetlinks.community.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.template.TemplateManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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
    public void handleTemplateModify(EntityModifyEvent<NotifyTemplateEntity> event) {
        reloadTemplate(event.getBefore());
    }

    @EventListener
    public void handleTemplateDelete(EntityDeletedEvent<NotifyTemplateEntity> event) {
        reloadTemplate(event.getEntity());
    }

    @EventListener
    public void handleConfigModify(EntityModifyEvent<NotifyConfigEntity> event) {
        reloadConfig(event.getBefore());
    }

    @EventListener
    public void handleConfigDelete(EntityDeletedEvent<NotifyConfigEntity> event) {
        reloadConfig(event.getEntity());
    }

    protected void reloadConfig(List<NotifyConfigEntity> entities) {
        Flux.fromIterable(entities)
                .map(NotifyConfigEntity::getId)
                .doOnNext(id -> log.info("clear notifier config [{}] cache", id))
                .flatMap(notifierManager::reload)
                .subscribe();
    }

    protected void reloadTemplate(List<NotifyTemplateEntity> entities) {
        Flux.fromIterable(entities)
                .map(NotifyTemplateEntity::getId)
                .doOnNext(id -> log.info("clear template [{}] cache", id))
                .flatMap(templateManager::reload)
                .subscribe();
    }

}
