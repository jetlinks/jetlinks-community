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
package org.jetlinks.community.notify;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@SuppressWarnings("all")
public class DefaultNotifierManager implements NotifierManager, CommandLineRunner, SmartInitializingSingleton {

    private final Map<String, Map<String, NotifierProvider>> providers = new ConcurrentHashMap<>();

    private ReactiveCacheContainer<String, Notifier> notifiers = ReactiveCacheContainer.create();

    private NotifyConfigManager configManager;

    protected EventBus eventBus;

    private final ApplicationContext context;

    public DefaultNotifierManager(EventBus eventBus, NotifyConfigManager manager,
                                  ApplicationContext context) {
        this.configManager = manager;
        this.eventBus = eventBus;
        this.context = context;
    }

    protected Mono<NotifierProperties> getProperties(NotifyType notifyType,
                                                     String id) {
        return configManager.getNotifyConfig(notifyType, id);
    }

    public Mono<Void> reload(String id) {
        return this
            .doReload(id)
            .then(eventBus.publish("/_sys/notifier/reload", id))
            .then();
    }

    private Mono<String> doReload(String id) {
        log.debug("reload notifer config {}", id);
        return Mono
            .justOrEmpty(notifiers.remove(id))
            .flatMap(Notifier::close)
            .thenReturn(id);
    }

    @Nonnull
    public Mono<Notifier> createNotifier(NotifierProperties properties) {
        return Mono
            .justOrEmpty(providers.get(properties.getType()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的通知类型:" + properties.getType())))
            .flatMap(map -> Mono.justOrEmpty(map.get(properties.getProvider())))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的服务商:" + properties.getProvider())))
            .flatMap(notifierProvider -> notifierProvider.createNotifier(properties))
            .map(this::wrapNotifier);
    }

    protected Notifier<?> wrapNotifier(Notifier<?> source) {
        return new NotifierEventDispatcher<>(eventBus, source);
    }

    @Override
    @Nonnull
    public Mono<Notifier> getNotifier(@Nonnull NotifyType type,
                                      @Nonnull String id) {

        return notifiers.computeIfAbsent(id, _id -> {
            return this.getProperties(type, id).flatMap(this::createNotifier);
        });
    }

    public void registerProvider(NotifierProvider provider) {
        providers.computeIfAbsent(provider.getType().getId(), ignore -> new ConcurrentHashMap<>())
                 .put(provider.getProvider().getId(), provider);
    }

    @Override
    public void run(String... args) throws Exception {

        eventBus
            .subscribe(
                Subscription.builder()
                            .subscriberId("notifier-loader")
                            .topics("/_sys/notifier/reload")
                            .justBroker()
                            .build(),
                String.class
            )
            .flatMap(id -> this
                .doReload(id)
                .onErrorResume(err -> {
                    log.error("reload notifer config error", err);
                    return Mono.empty();
                }))
            .subscribe();

    }

    @Override
    public void afterSingletonsInstantiated() {
        context.getBeanProvider(NotifierProvider.class)
               .forEach(this::registerProvider);
    }
}
