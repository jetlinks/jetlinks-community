package org.jetlinks.community.notify.manager.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.community.notify.Notifier;
import org.jetlinks.community.notify.NotifierProvider;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@AllArgsConstructor
public class NotifyValidationHandler {

    private final List<NotifierProvider> notifierProviders;
    private final List<TemplateProvider> templateProviders;

    @EventListener
    public void handleConfigEvent(EntityCreatedEvent<NotifyConfigEntity> event){
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::validateConfig)
        );
    }

    @EventListener
    public void handleConfigEvent(EntitySavedEvent<NotifyConfigEntity> event){
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::validateConfig)
        );
    }

    @EventListener
    public void handleConfigEvent(EntityModifyEvent<NotifyConfigEntity> event){
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(this::validateConfig)
        );
    }


    @EventListener
    public void handleTemplateEvent(EntityCreatedEvent<NotifyTemplateEntity> event){
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::validateTemplate)
        );
    }

    @EventListener
    public void handleTemplateEvent(EntitySavedEvent<NotifyTemplateEntity> event){
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::validateTemplate)
        );
    }

    @EventListener
    public void handleTemplateEvent(EntityModifyEvent<NotifyTemplateEntity> event){
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(this::validateTemplate)
        );
    }

    public Mono<Void> validateTemplate(NotifyTemplateEntity entity) {
        return this
            .getTemplateProvider(entity.getType(), entity.getProvider())
            .createTemplate(entity.toTemplateProperties())
            .then();
    }

    public Mono<Void> validateConfig(NotifyConfigEntity entity) {
        return this
            .getNotifierProvider(entity.getType(), entity.getProvider())
            .createNotifier(entity.toProperties())
            .flatMapMany(Notifier::close)
            .then();
    }


    public NotifierProvider getNotifierProvider(String type, String provider) {
        for (NotifierProvider prov : notifierProviders) {
            if (prov.getType().getId().equalsIgnoreCase(type) && prov
                .getProvider()
                .getId()
                .equalsIgnoreCase(provider)) {
                return prov;
            }
        }
        throw new ValidationException("error.unsupported_notify_provider");
    }

    public TemplateProvider getTemplateProvider(String type, String provider) {
        for (TemplateProvider prov : templateProviders) {
            if (prov.getType().getId().equalsIgnoreCase(type) && prov
                .getProvider()
                .getId()
                .equalsIgnoreCase(provider)) {
                return prov;
            }
        }
        throw new ValidationException("error.unsupported_notify_provider");
    }
}
