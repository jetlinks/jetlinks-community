package org.jetlinks.community.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberEntity;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.core.event.EventBus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotifySubscriberService extends GenericReactiveCrudService<NotifySubscriberEntity, String> implements CommandLineRunner {

    private final EventBus eventBus;

    private final ClusterManager clusterManager;

    private final Map<String, SubscriberProvider> providers = new ConcurrentHashMap<>();

    private final Map<String, Disposable> subscribers = new ConcurrentHashMap<>();

    public NotifySubscriberService(EventBus eventBus,
                                   ClusterManager clusterManager,
                                   List<SubscriberProvider> providers) {
        this.eventBus = eventBus;
        this.clusterManager = clusterManager;
        for (SubscriberProvider provider : providers) {
            this.providers.put(provider.getId(), provider);
        }
    }

    public Optional<SubscriberProvider> getProvider(String provider) {
        return Optional.ofNullable(provider).map(providers::get);
    }

    private void doStart() {
        clusterManager.<NotifySubscriberEntity>getTopic("notification-changed")
            .subscribe()
            .subscribe(this::handleSubscribe);
    }

    protected void doNotifyChange(NotifySubscriberEntity entity) {
        clusterManager.<NotifySubscriberEntity>getTopic("notification-changed")
            .publish(Mono.just(entity))
            .retry(3)
            .subscribe();
    }

    @EventListener
    public void handleEvent(EntityCreatedEvent<NotifySubscriberEntity> entity) {
        entity.getEntity().forEach(this::doNotifyChange);
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<NotifySubscriberEntity> entity) {
        entity.getEntity().forEach(this::doNotifyChange);
    }

    @EventListener
    public void handleEvent(EntityDeletedEvent<NotifySubscriberEntity> entity) {
        entity.getEntity().forEach(e -> {
            e.setState(SubscribeState.disabled);
            doNotifyChange(e);
        });
    }

    @EventListener
    public void handleEvent(EntityModifyEvent<NotifySubscriberEntity> entity) {
        entity.getAfter().forEach(this::doNotifyChange);
    }

    private void handleSubscribe(NotifySubscriberEntity entity) {

        //取消订阅
        if (entity.getState() == SubscribeState.disabled) {
            Optional.ofNullable(subscribers.remove(entity.getId()))
                .ifPresent(Disposable::dispose);
            log.debug("unsubscribe:{}({}),{}", entity.getTopicProvider(), entity.getTopicName(), entity.getId());
            return;
        }

        //模版
        Notification template = Notification.from(entity);
        //转发通知
        String dispatch = template.createTopic();

        Disposable old = subscribers
            .put(entity.getId(),
                Mono.zip(ReactiveAuthenticationHolder.get(entity.getSubscriber()), Mono.justOrEmpty(getProvider(entity.getTopicProvider())))
                    .flatMap(tp2 -> tp2.getT2().createSubscriber(entity.getId(),tp2.getT1(), entity.getTopicConfig()))
                    .flatMap(subscriber ->
                        subscriber
                            .subscribe()
                            .map(template::copyWithMessage)
                            .flatMap(notification -> eventBus.publish(dispatch, notification))
                            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
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
        return Mono.justOrEmpty(getProvider(entity.getTopicProvider()))
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("不支持的主题:" + entity.getTopicProvider())))
            .map(provider -> {
                entity.setTopicName(provider.getName());
                return entity;
            })
            .flatMap(subEntity -> {
                if (StringUtils.isEmpty(entity.getId())) {
                    entity.setId(null);
                    return save(Mono.just(entity));
                } else {
                    return createUpdate().set(entity)
                        .where(NotifySubscriberEntity::getId, entity.getId())
                        .and(NotifySubscriberEntity::getSubscriberType, entity.getSubscriberType())
                        .and(NotifySubscriberEntity::getSubscriber, entity.getSubscriber())
                        .execute();
                }
            }).then();

    }

    @Override
    public void run(String... args) {
        doStart();
        createQuery()
            .where(NotifySubscriberEntity::getState, SubscribeState.enabled)
            .fetch()
            .doOnNext(this::handleSubscribe)
            .subscribe();
    }
}
