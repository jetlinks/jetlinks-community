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
package org.jetlinks.community.notify.configuration;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public class CompositeTemplateManager implements TemplateManager {

    private final List<TemplateManager> managers;

    private <T> Mono<T> doWith(Function<TemplateManager, Mono<T>> executor) {
        Mono<T> mono = null;

        for (TemplateManager manager : managers) {
            if (mono == null) {
                mono = executor.apply(manager);
            } else {
                mono = mono.switchIfEmpty(executor.apply(manager));
            }
        }

        return mono == null ? Mono.empty() : mono;

    }

    @Nonnull
    @Override
    public Mono<? extends Template> getTemplate(@Nonnull NotifyType type, @Nonnull String id) {
        return this
            .doWith(manager -> manager
                .getTemplate(type, id)
                .onErrorResume(UnsupportedOperationException.class, err -> Mono.empty()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("通知类型不支持:" + type.getId())));
    }

    @Nonnull
    @Override
    public Mono<? extends Template> createTemplate(@Nonnull NotifyType type, @Nonnull TemplateProperties properties) {
        return this
            .doWith(manager -> manager
                .createTemplate(type, properties)
                .onErrorResume(UnsupportedOperationException.class, err -> Mono.empty()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("通知类型不支持:" + type.getId())));
    }

    @Nonnull
    @Override
    public Mono<Void> reload(String templateId) {
        return doWith(manager -> manager.reload(templateId));
    }
}
