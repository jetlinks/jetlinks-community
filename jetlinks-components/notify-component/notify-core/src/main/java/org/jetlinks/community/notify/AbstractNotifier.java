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

import lombok.AllArgsConstructor;
import org.jetlinks.core.Values;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.relation.RelationManagerHolder;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@AllArgsConstructor
public abstract class AbstractNotifier<T extends Template> implements Notifier<T> {

    private final TemplateManager templateManager;

    @Override
    @Nonnull
    public Mono<Void> send(@Nonnull String templateId, @Nonnull Values context) {
        return templateManager
            .getTemplate(getType(), templateId)
            .switchIfEmpty(Mono.error(new UnsupportedOperationException("模版不存在:" + templateId)))
            .flatMap(tem -> send((T) tem, context));
    }


}
