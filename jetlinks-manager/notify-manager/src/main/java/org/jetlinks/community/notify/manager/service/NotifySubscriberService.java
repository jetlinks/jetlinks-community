package org.jetlinks.community.notify.manager.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.utils.CompositeMap;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.manager.configuration.NotifySubscriberProperties;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberChannelEntity;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberEntity;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberProviderEntity;
import org.jetlinks.community.notify.manager.enums.NotifyChannelState;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProviders;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.community.utils.ReactorUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotifySubscriberService extends GenericReactiveCrudService<NotifySubscriberEntity, String>
    implements CommandLineRunner {

    private final EventBus eventBus;

    private final Map<String, SubTable> subscribers = new ConcurrentHashMap<>();

    private final ApplicationEventPublisher eventPublisher;

    private final NotifySubscriberProviderService providerService;
    private final ReactiveRepository<NotifySubscriberChannelEntity, String> channelRepository;

    private final NotifySubscriberProperties properties;

    //生效的通道信息 key为provider,value为通道list
    private final Map<String, NotifySubscriberProviderCache> providerChannels = new ConcurrentHashMap<>();

    public NotifySubscriberService(EventBus eventBus,
                                   ObjectProvider<SubscriberProvider> providers,
                                   ApplicationEventPublisher eventPublisher,
                                   NotifySubscriberProviderService providerService,
                                   ReactiveRepository<NotifySubscriberChannelEntity, String> channelRepository,
                                   NotifySubscriberProperties properties) {
        this.eventBus = eventBus;
        this.eventPublisher = eventPublisher;
        this.providerService = providerService;
        this.properties = properties;
        this.channelRepository = channelRepository;
        for (SubscriberProvider provider : providers) {
            SubscriberProviders.register(provider);
        }
    }

    @EventListener
    public void handleChannelEvent(EntityCreatedEvent<NotifySubscriberChannelEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity ->
                         providerChannels
                             .computeIfAbsent(providerEntity.getProviderId(), ignore -> new NotifySubscriberProviderCache())
                             .addChannel(providerEntity)
                )
        );
    }

    @EventListener
    public void handleChannelEvent(EntityModifyEvent<NotifySubscriberChannelEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getAfter())
                .map(providerEntity ->
                         providerChannels
                             .computeIfAbsent(providerEntity.getProviderId(), ignore -> new NotifySubscriberProviderCache())
                             .addChannel(providerEntity)
                )
        );
    }

    @EventListener
    public void handleChannelEvent(EntitySavedEvent<NotifySubscriberChannelEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity ->
                         providerChannels
                             .computeIfAbsent(providerEntity.getProviderId(), ignore -> new NotifySubscriberProviderCache())
                             .addChannel(providerEntity)
                )
        );
    }

    @EventListener
    public void handleChannelEvent(EntityDeletedEvent<NotifySubscriberChannelEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity ->
                         providerChannels
                             .computeIfAbsent(providerEntity.getProviderId(), ignore -> new NotifySubscriberProviderCache())
                             .removeChannel(providerEntity)
                )
        );
    }

    @EventListener
    public void handleEvent(EntityCreatedEvent<NotifySubscriberProviderEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity ->
                         providerChannels
                             .computeIfAbsent(providerEntity.getId(), ignore -> new NotifySubscriberProviderCache())
                             .update(providerEntity)
                )
        );
    }

    @EventListener
    public void handleEvent(EntityModifyEvent<NotifySubscriberProviderEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getAfter())
                .map(providerEntity ->
                         providerChannels
                             .computeIfAbsent(providerEntity.getId(), ignore -> new NotifySubscriberProviderCache())
                             .update(providerEntity)
                )
        );
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<NotifySubscriberProviderEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity ->
                         providerChannels
                             .computeIfAbsent(providerEntity.getId(), ignore -> new NotifySubscriberProviderCache())
                             .update(providerEntity)
                )
        );
    }

    @EventListener
    public void handleEvent(EntityDeletedEvent<NotifySubscriberProviderEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity -> {
                    ReactorUtils.dispose(providerChannels.remove(providerEntity.getId()));
                    return Mono.empty();
                })
        );
    }


    @EventListener
    public void handleSubscriberEvent(EntityDeletedEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity -> {
                    providerEntity.setState(SubscribeState.disabled);
                    handleSubscribe(providerEntity);
                    return Mono.empty();
                })
        );
    }

    @EventListener
    public void handleSubscriberEvent(EntitySavedEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity -> {
                    handleSubscribe(providerEntity);
                    return Mono.empty();
                })
        );
    }

    @EventListener
    public void handleSubscriberEvent(EntityModifyEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getAfter())
                .map(providerEntity -> {
                    handleSubscribe(providerEntity);
                    return Mono.empty();
                })
        );
    }

    @EventListener
    public void handleSubscriberEvent(EntityCreatedEvent<NotifySubscriberEntity> entity) {
        entity.async(
            Flux.fromIterable(entity.getEntity())
                .map(providerEntity -> {
                    handleSubscribe(providerEntity);
                    return Mono.empty();
                })
        );
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

    public Mono<SubscriberProvider> getProvider(NotifySubscriberEntity entity) {

        //since 2.1 新的订阅方式
        if (StringUtils.hasText(entity.getProviderId())) {
            return providerService
                .findById(entity.getProviderId())
                .mapNotNull(e -> SubscriberProviders
                    .getProvider(e.getProvider())
                    .map(pro -> new SubscriberProviderProxy(pro, e.getConfiguration()))
                    .orElse(null));

        }

        return Mono.justOrEmpty(SubscriberProviders.getProvider(entity.getTopicProvider()));
    }


    @AllArgsConstructor
    static class SubscriberProviderProxy implements SubscriberProvider {
        private final SubscriberProvider proxy;
        private final Map<String, Object> config;

        @Override
        public String getId() {
            return proxy.getId();
        }

        @Override
        public String getName() {
            return proxy.getName();
        }

        @Override
        public Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config) {
            if (MapUtils.isNotEmpty(this.config)) {
                config = new CompositeMap<>(config, this.config);
            }
            return proxy.createSubscriber(id, authentication, config);
        }

        @Override
        public ConfigMetadata getConfigMetadata() {
            return proxy.getConfigMetadata();
        }
    }


    private void handleSubscribe(NotifySubscriberEntity entity) {


        subscribers.compute(entity.getSubscriber(), (id, table) -> {
            if (table == null) {
                table = new SubTable(id);
            }
            table.handleSubscriberEntity(entity);

            if (table.subs.isEmpty()) {
                table.dispose();
                return null;
            }

            return table;
        });

    }

    @Transactional(rollbackFor = Throwable.class)
    public Mono<Void> doSubscribe(NotifySubscriberEntity entity) {
        return this
            .getProvider(entity)
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.unsupported_topics", 500, entity.getTopicProvider())))
            .map(provider -> {
                entity.setTopicName(provider.getName());
                return entity;
            })
            .flatMap(subEntity -> {
                if (!StringUtils.hasText(entity.getId())) {
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
            })
            .then();

    }

    //用户权限变更时重新订阅
    @Subscribe(value = Topics.Authentications.allUserAuthenticationChanged, features = Subscription.Feature.local)
    public void handleAuthenticationChanged(Authentication auth) {

        SubTable table = subscribers.get(auth.getUser().getId());
        if (table != null) {
            table.resubscribe(auth);
        }
    }

    //用户订阅表
    private class SubTable implements Disposable {
        private final String subscriber;

        //订阅信息 key为订阅id
        private final Map<String, Node> subs = new ConcurrentHashMap<>();

        private final Disposable.Composite disposable = Disposables.composite();

        public SubTable(String subscriber) {
            this.subscriber = subscriber;
        }

        void resubscribe(Authentication authentication) {

            for (Node value : subs.values()) {
                value.resubscribe(authentication);
            }
        }

        Mono<Void> unsubscribe(NotifySubscriberProviderEntity entity) {
            for (Node node : subs.values()) {
                if (Objects.equals(node.providerId, entity.getId())) {
                    node.removeChannels();
                }
            }
            return Mono.empty();
        }

        Mono<Void> unsubscribe(NotifySubscriberChannelEntity entity) {

            for (Node node : subs.values()) {
                if (Objects.equals(node.providerId, entity.getProviderId())) {
                    node.removeChannel(entity);
                }
            }
            return Mono.empty();
        }

        Mono<Void> resubscribe(NotifySubscriberChannelEntity entity) {

            List<Node> nodes = subs
                .values()
                .stream()
                .filter(node -> Objects.equals(node.providerId, entity.getProviderId()))
                .collect(Collectors.toList());

            if (nodes.isEmpty()) {
                return Mono.empty();
            }

            return ReactiveAuthenticationHolder
                .get(subscriber)
                .flatMap(auth -> Flux
                    .fromIterable(nodes)
                    .doOnNext(node -> node.resubscribe(entity, auth))
                    .then());
        }

        Mono<Void> resubscribe(NotifySubscriberProviderEntity entity) {

            List<Node> nodes = subs
                .values()
                .stream()
                .filter(node -> Objects.equals(node.providerId, entity.getId()))
                .collect(Collectors.toList());

            if (nodes.isEmpty()) {
                return Mono.empty();
            }

            return ReactiveAuthenticationHolder
                .get(subscriber)
                .flatMap(auth -> Flux
                    .fromIterable(nodes)
                    .doOnNext(node -> node.resubscribe(entity, auth))
                    .then());
        }

        public void handleSubscriberEntity(NotifySubscriberEntity entity) {
            //取消订阅
            if (entity.getState() == SubscribeState.disabled) {
                Disposable disp = subs.remove(entity.getId());
                if (disp != null) {
                    log.debug("unsubscribe:{}({}),{},subscriber:{}",
                              entity.getTopicProvider(),
                              entity.getTopicName(),
                              entity.getId(),
                              subscriber);
                    disp.dispose();
                }

                return;
            }

            subs.computeIfAbsent(entity.getId(), ignore -> new Node())
                .init(entity);
        }

        @Override
        public void dispose() {
            disposable.dispose();
        }

        @Override
        public boolean isDisposed() {
            return disposable.isDisposed();
        }

        class Node implements Disposable {

            private String providerId;

            private Disposable disposable;

            private Notification template;

            //用户配置的通道
            private Set<String> userConfigureNotifyChannels;

            public void init(NotifySubscriberEntity entity) {
                providerId = entity.getProviderId();
                if (entity.getNotifyChannels() != null) {
                    userConfigureNotifyChannels = new HashSet<>(entity.getNotifyChannels());
                }
                //模版
                template = Notification.from(entity);

                if (null != disposable) {
                    log.debug("close old subscriber:{}({})", template.getTopicProvider(), template.getTopicName());
                    disposable.dispose();
                }

                disposable = Mono
                    .zip(ReactiveAuthenticationHolder.get(entity.getSubscriber()), getProvider(entity))
                    .flatMap(tp2 -> {
                        initNotifyChannels(tp2.getT1());
                        return tp2.getT2().createSubscriber(entity.getId(), tp2.getT1(), entity.getTopicConfig());
                    })
                    .flatMap(subscriber ->
                                 subscriber
                                     .subscribe(entity.toLocale())
                                     .map(template::copyWithMessage)
                                     .doOnNext(eventPublisher::publishEvent)
                                     .onErrorResume((err) -> {
                                         log.error(err.getLocalizedMessage(), err);
                                         return Mono.empty();
                                     })
                                     .then())
                    .subscribe();
            }

            private void initNotifyChannels(Authentication authentication) {
                if (providerId == null) {
                    return;
                }

                NotifySubscriberProviderCache cache = providerChannels.get(providerId);
                if (cache != null) {
                    resubscribe(cache.provider, authentication);
                    for (NotifySubscriberChannelEntity value : cache.channels.values()) {
                        resubscribe(value, authentication);
                    }
                }
            }


            public synchronized void removeChannels() {
                List<String> effectNotifyChannel = template.getNotifyChannels();
                if (effectNotifyChannel != null) {
                    template.setNotifyChannels(Collections.emptyList());
                }
            }

            public synchronized void removeChannel(NotifySubscriberChannelEntity e) {
                List<String> effectNotifyChannel = template.getNotifyChannels();
                if (effectNotifyChannel != null) {
                    effectNotifyChannel = new ArrayList<>(effectNotifyChannel);
                    effectNotifyChannel.remove(e.getId());
                    template.setNotifyChannels(effectNotifyChannel);
                }
            }

            public void resubscribe(Authentication auth) {
                if (providerId == null) {
                    return;
                }
                NotifySubscriberProviderCache cache = providerChannels.get(providerId);
                if (null != cache) {
                    resubscribe(cache.provider, auth);
                } else {
                    removeChannels();
                }
            }

            public synchronized void resubscribe(NotifySubscriberProviderEntity e, Authentication auth) {
                if (e.getState() == NotifyChannelState.disabled
                    || (!properties.isAllowAllNotify(auth)  && e.getGrant() != null && !e.getGrant().isGranted(auth))) {
                    removeChannels();
                } else {
                    //重新设置通知通道
                    NotifySubscriberProviderCache cache = providerChannels.get(e.getId());
                    if (cache != null) {
                        template.setNotifyChannels(
                            userConfigureNotifyChannels
                                .stream()
                                .filter(channelId -> cache.hasChannelAccess(auth, channelId))
                                .collect(Collectors.toList()));
                    }
                }
            }

            public synchronized void resubscribe(NotifySubscriberChannelEntity e, Authentication auth) {

                List<String> effectNotifyChannel = template.getNotifyChannels();
                if (effectNotifyChannel == null || userConfigureNotifyChannels == null) {
                    return;
                }
                Set<String> newChannels = new HashSet<>(effectNotifyChannel);
                //通道被禁用或者没有权限则删除此通道
                if (e.getState() == NotifyChannelState.disabled
                    || (!properties.isAllowAllNotify(auth)  && e.getGrant() != null && !e.getGrant().isGranted(auth))) {
                    newChannels.remove(e.getId());
                } else {
                    if (userConfigureNotifyChannels.contains(e.getId())) {
                        newChannels.add(e.getId());
                    }
                }
                template.setNotifyChannels(new ArrayList<>(newChannels));
            }

            @Override
            public void dispose() {
                if (null != disposable) {
                    disposable.dispose();
                    disposable = null;
                }
            }
        }
    }

    class NotifySubscriberProviderCache implements Disposable {
        private NotifySubscriberProviderEntity provider;

        private final Map<String, NotifySubscriberChannelEntity> channels = new ConcurrentHashMap<>();

        public Mono<Void> update(NotifySubscriberProviderEntity entity) {
            provider = entity;
            return resubscribe(entity);
        }

        public boolean hasChannelAccess(Authentication auth, String channelId) {
            NotifySubscriberChannelEntity channel = channels.get(channelId);
            if (channel == null || channel.getState() == NotifyChannelState.disabled) {
                return false;
            }
            if (!properties.isAllowAllNotify(auth) && channel.getGrant() != null) {
                return channel.getGrant().isGranted(auth);
            }
            return true;
        }

        public Mono<Void> addChannel(NotifySubscriberChannelEntity channel) {
            channels.put(channel.getId(), channel);
            return resubscribe(channel);
        }

        public Mono<Void> removeChannel(NotifySubscriberChannelEntity channel) {
            channels.remove(channel.getId());
            return unsubscribe(channel);
        }

        private Mono<Void> unsubscribe(NotifySubscriberChannelEntity channel) {
            return Flux
                .fromIterable(subscribers.values())
                .flatMap(table -> table.unsubscribe(channel))
                .then();
        }

        private Mono<Void> unsubscribe(NotifySubscriberProviderEntity provider) {
            return Flux
                .fromIterable(subscribers.values())
                .flatMap(table -> table.unsubscribe(provider))
                .then();
        }

        private Mono<Void> resubscribe(NotifySubscriberChannelEntity channel) {
            return Flux
                .fromIterable(subscribers.values())
                .flatMap(table -> table.resubscribe(channel))
                .then();
        }

        private Mono<Void> resubscribe(NotifySubscriberProviderEntity channel) {
            return Flux
                .fromIterable(subscribers.values())
                .flatMap(table -> table.resubscribe(channel))
                .then();
        }


        @Override
        public void dispose() {
            if (null != provider) {
                unsubscribe(provider).subscribe();
            }
        }
    }

    @Override
    public void run(String... args) {

        Flux.concat(
                providerService
                    .createQuery()
                    .fetch()
                    .flatMap(e -> providerChannels
                        .computeIfAbsent(e.getId(), ignore -> new NotifySubscriberProviderCache())
                        .update(e)
                        .onErrorResume(err -> Mono.empty())),
                channelRepository
                    .createQuery()
                    .fetch()
                    .flatMap(e -> providerChannels
                        .computeIfAbsent(e.getProviderId(), ignore -> new NotifySubscriberProviderCache())
                        .addChannel(e)
                        .onErrorResume(err -> Mono.empty())),
                createQuery()
                    .where(NotifySubscriberEntity::getState, SubscribeState.enabled)
                    .fetch()
                    .doOnNext(this::handleSubscribe)
            )
            .subscribe();

    }
}
