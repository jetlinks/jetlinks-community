package org.jetlinks.community.notify.manager.service;


import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberEntity;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProviders;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotifySubscriberService extends GenericReactiveCrudService<NotifySubscriberEntity, String> implements CommandLineRunner {

    private final EventBus eventBus;

    private final Map<String, SubscriberProvider> providers = new ConcurrentHashMap<>();

    private final Map<String, Disposable> subscribers = new ConcurrentHashMap<>();

    private final ApplicationEventPublisher eventPublisher;

    public NotifySubscriberService(EventBus eventBus,
                                   ObjectProvider<SubscriberProvider> providers,
                                   ApplicationEventPublisher eventPublisher) {
        this.eventBus = eventBus;
        this.eventPublisher=eventPublisher;
        for (SubscriberProvider provider : providers) {
            this.providers.put(provider.getId(), provider);
            SubscriberProviders.register(provider);
        }
    }

    public Optional<SubscriberProvider> getProvider(String provider) {
        return Optional.ofNullable(provider).map(providers::get);
    }

    protected Mono<Void> doNotifyChange(NotifySubscriberEntity entity) {
        return eventBus
            .publish("/notification-changed", entity)
            .then();

    }

    @EventListener
    public void handleEvent(EntityPrepareCreateEvent<NotifySubscriberEntity> entity) {
        //填充语言
        entity.async(
            LocaleUtils
                .currentReactive()
                .doOnNext(locale -> {
                    for (NotifySubscriberEntity subscriber : entity.getEntity()) {
                        if (subscriber.getLocale() == null) {
                            subscriber.setLocale(locale.toLanguageTag());
                        }
                    }
                })
        );
    }

    @EventListener
    public void handleEvent(EntityPrepareSaveEvent<NotifySubscriberEntity> entity) {
        //填充语言
        entity.async(
            LocaleUtils
                .currentReactive()
                .doOnNext(locale -> {
                    for (NotifySubscriberEntity subscriber : entity.getEntity()) {
                        if (subscriber.getLocale() == null) {
                            subscriber.setLocale(locale.toLanguageTag());
                        }
                    }
                })
        );
    }

    @EventListener
    public void handleEvent(EntityCreatedEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .flatMap(this::doNotifyChange)
        );
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .flatMap(this::doNotifyChange)
        );
    }

    @EventListener
    public void handleEvent(EntityDeletedEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .doOnNext(e -> e.setState(SubscribeState.disabled))
                .flatMap(this::doNotifyChange)
        );

    }

    @EventListener
    public void handleEvent(EntityModifyEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getAfter())
                .flatMap(this::doNotifyChange)
        );
    }

    @Subscribe("/notification-changed")
    public void handleSubscribe(NotifySubscriberEntity entity) {

        //取消订阅
        if (entity.getState() == SubscribeState.disabled) {
            Optional.ofNullable(subscribers.remove(entity.getId()))
                    .ifPresent(Disposable::dispose);
            log.debug("unsubscribe:{}({}),{}", entity.getTopicProvider(), entity.getTopicName(), entity.getId());
            return;
        }

        //模版
        Notification template = Notification.from(entity);

        Disposable old = subscribers
            .put(entity.getId(),
                 Mono
                     .zip(ReactiveAuthenticationHolder.get(entity.getSubscriber()), Mono.justOrEmpty(getProvider(entity.getTopicProvider())))
                     .flatMap(tp2 -> tp2.getT2().createSubscriber(entity.getId(), tp2.getT1(), entity.getTopicConfig()))
                     .flatMap(subscriber ->
                                  subscriber
                                      .subscribe(entity.toLocale())
                                      .map(template::copyWithMessage)
                                      .doOnNext(eventPublisher::publishEvent)
                                      .onErrorResume((err) -> {
                                          log.error(err.getMessage(), err);
                                          return Mono.empty();
                                      })
                                      .then())
                     .subscribe()
            );
        log.debug("subscribe :{}({})", template.getTopicProvider(), template.getTopicName());

        if (null != old) {
            log.debug("close old subscriber:{}({})", template.getTopicProvider(), template.getTopicName());
            old.dispose();
        }
    }

    public Mono<Void> doSubscribe(NotifySubscriberEntity entity) {
        return Mono
            .justOrEmpty(getProvider(entity.getTopicProvider()))
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.unsupported_topics", 500, entity.getTopicProvider())))
            .map(provider -> {
                entity.setTopicName(provider.getName());
                return entity;
            })
            .flatMap(subEntity -> {
                if (StringUtils.isEmpty(entity.getId())) {
                    entity.setId(null);
                    return save(entity);
                } else {
                    return createUpdate()
                        .set(entity)
                        .where(NotifySubscriberEntity::getId, entity.getId())
                        .and(NotifySubscriberEntity::getSubscriberType, entity.getSubscriberType())
                        .and(NotifySubscriberEntity::getSubscriber, entity.getSubscriber())
                        .execute();
                }
            }).then();

    }

    @Override
    public void run(String... args) {
        createQuery()
            .where(NotifySubscriberEntity::getState, SubscribeState.enabled)
            .fetch()
            .doOnNext(this::handleSubscribe)
            .subscribe();
    }
}
