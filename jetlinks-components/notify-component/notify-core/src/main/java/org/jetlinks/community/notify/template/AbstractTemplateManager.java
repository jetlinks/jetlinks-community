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
package org.jetlinks.community.notify.template;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.boot.CommandLineRunner;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
public abstract class AbstractTemplateManager implements TemplateManager, CommandLineRunner {

    protected final Map<String, Map<String, TemplateProvider>> providers = new ConcurrentHashMap<>();

    protected final ReactiveCacheContainer<String, Template> templates = ReactiveCacheContainer.create();

    protected abstract Mono<TemplateProperties> getProperties(NotifyType type, String id);

    private EventBus eventBus;

    protected void register(TemplateProvider provider) {
        providers.computeIfAbsent(provider.getType().getId(), ignore -> new ConcurrentHashMap<>())
                 .put(provider.getProvider().getId(), provider);
    }

    @Override
    @Nonnull
    public Mono<? extends Template> createTemplate(@Nonnull NotifyType type, @Nonnull TemplateProperties prop) {
        return Mono.justOrEmpty(providers.get(type.getId()))
                   .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的通知类型:" + prop.getType())))
                   .flatMap(map -> Mono
                       .justOrEmpty(map.get(prop.getProvider()))
                       .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的服务商:" + prop.getProvider())))
                       .flatMap(provider -> provider.createTemplate(prop)));
    }

    @Nonnull
    @Override
    public Mono<? extends Template> getTemplate(@Nonnull NotifyType type, @Nonnull String id) {
        return templates.computeIfAbsent(id, _id -> this
            .getProperties(type, _id)
            .flatMap(prop -> this.createTemplate(type, prop)));
    }

    @Override
    @Nonnull
    public Mono<Void> reload(String templateId) {
        return doReload(templateId)
            .then(eventBus.publish("/_sys/notifier-temp/reload", templateId))
            .then();
    }

    private Mono<String> doReload(String templateId) {
        log.debug("reload notify template {}", templateId);
        return Mono.justOrEmpty(templates.remove(templateId))
                   .thenReturn(templateId);
    }

    @Override
    public void run(String... args) {

        eventBus
            .subscribe(
                Subscription.builder()
                            .subscriberId("notifier-template-loader")
                            .topics("/_sys/notifier-temp/reload")
                            .justBroker()
                            .build(),
                String.class
            )
            .flatMap(id -> this
                .doReload(id)
                .onErrorResume(err -> {
                    log.error("reload notify template config error", err);
                    return Mono.empty();
                }))
            .subscribe();
    }
}
