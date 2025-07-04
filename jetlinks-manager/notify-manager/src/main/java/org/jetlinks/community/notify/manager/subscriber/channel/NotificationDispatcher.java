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
package org.jetlinks.community.notify.manager.subscriber.channel;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberChannelEntity;
import org.jetlinks.community.notify.manager.enums.NotifyChannelState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知订阅转发器,将通知信息转发到对应的订阅通道中
 *
 * @author zhouhao
 * @since 2.0
 */
@Component
@Slf4j
public class NotificationDispatcher implements CommandLineRunner {

    private final EventBus eventBus;

    private final ReactiveCacheContainer<String, NotifyChannel> channels = ReactiveCacheContainer.create();

    private final Map<String, NotifyChannelProvider> providers = new HashMap<>();

    private final ReactiveRepository<NotifySubscriberChannelEntity, String> channelRepository;

    public NotificationDispatcher(EventBus eventBus,
                                  ObjectProvider<NotifyChannelProvider> providers,
                                  ReactiveRepository<NotifySubscriberChannelEntity, String> channelRepository) {
        this.eventBus = eventBus;
        this.channelRepository = channelRepository;
        //默认支持站内信
        this.channels.put(InsideMailChannelProvider.provider, new InsideMailChannelProvider(eventBus));

        for (NotifyChannelProvider provider : providers) {
            this.providers.put(provider.getId(), provider);
        }
    }

    @EventListener
    public void handleNotifications(Notification notification) {

        List<String> channelIdList = notification.getNotifyChannels();
        //默认站内信
        if (channelIdList == null) {
            channelIdList = Collections.singletonList(InsideMailChannelProvider.provider);
        }
        //发送通知
        for (String notifyChannel : channelIdList) {
            NotifyChannel dispatcher = channels.getNow(notifyChannel);
            if (dispatcher != null) {
                dispatcher
                    .sendNotify(notification)
                    .subscribe();
            }
        }

    }

    @EventListener
    public void handleEvent(EntityCreatedEvent<NotifySubscriberChannelEntity> event) {

        event.async(
            register(event.getEntity())
        );
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<NotifySubscriberChannelEntity> event) {

        event.async(
            register(event.getEntity())
        );
    }

    @EventListener
    public void handleEvent(EntityModifyEvent<NotifySubscriberChannelEntity> event) {

        event.async(
            register(event.getAfter())
        );
    }

    @EventListener
    public void handleEvent(EntityDeletedEvent<NotifySubscriberChannelEntity> event) {
        event.async(
            unregister(event.getEntity())
        );
    }

    @Subscribe(value = "/_sys/notify-channel/unregister", features = Subscription.Feature.broker)
    public void unregister(NotifySubscriberChannelEntity entity) {
        channels.remove(entity.getId());
    }

    @Subscribe(value = "/_sys/notify-channel/register", features = Subscription.Feature.broker)
    public Mono<Void> register(NotifySubscriberChannelEntity entity) {
        if (entity.getState() == NotifyChannelState.disabled) {
            channels.remove(entity.getId());
        } else {
            return channels
                .compute(entity.getId(), (ignore, old) -> {
                    if (null != old) {
                        old.dispose();
                    }
                    return createChannel(entity);
                })
                .then();
        }
        return Mono.empty();
    }

    private Mono<NotifyChannel> createChannel(NotifySubscriberChannelEntity entity) {
        NotifyChannelProvider provider = providers.get(entity.getChannelProvider());
        if (null == provider) {
            return Mono.empty();
        }
        return provider.createChannel(entity.getChannelConfiguration());
    }

    private Mono<Void> unregister(List<NotifySubscriberChannelEntity> entities) {
        for (NotifySubscriberChannelEntity entity : entities) {
            unregister(entity);
        }
        return Flux.fromIterable(entities)
                   .flatMap(e -> eventBus.publish("/_sys/notify-channel/unregister", e))
                   .then();
    }

    private Mono<Void> register(List<NotifySubscriberChannelEntity> entities) {
        return Flux.fromIterable(entities)
                   .flatMap(e -> register(e)
                       .then(eventBus.publish("/_sys/notify-channel/register", e)))
                   .then();

    }

    @Override
    public void run(String... args) throws Exception {
        channelRepository
            .createQuery()
            .where(NotifySubscriberChannelEntity::getState, NotifyChannelState.enabled)
            .fetch()
            .flatMap(e -> this
                .register(e)
                .onErrorResume(er -> {
                    log.warn("register notify channel error", er);
                    return Mono.empty();
                }))
            .subscribe();

    }
}
