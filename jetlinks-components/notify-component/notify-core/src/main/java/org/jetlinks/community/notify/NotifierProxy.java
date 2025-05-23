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
import org.jetlinks.community.notify.event.NotifierEvent;
import org.jetlinks.community.notify.template.Template;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@AllArgsConstructor
public abstract class NotifierProxy<T extends Template> implements Notifier<T> {

    private final Notifier<T> target;

    @Override
    public String getNotifierId() {
        return target.getNotifierId();
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return target.getType();
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return target.getProvider();
    }

    @Override
    public <R> R unwrap(Class<R> type) {
        return target.unwrap(type);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return target.isWrapperFor(type);
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull String templateId, Values context) {
        return target
            .send(templateId, context)
            .then(Mono.defer(() -> onSuccess(templateId, context)))
            .onErrorResume(err -> onError(templateId, context, err).then(Mono.error(err)));
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull T template, @Nonnull Values context) {
        return target
            .send(template, context)
            .then(Mono.defer(() -> onSuccess(template, context)))
            .onErrorResume(err -> onError(template, context, err).then(Mono.error(err)));
    }

    protected Mono<Void> onError(T template, Values ctx, Throwable error) {
        return onEvent(NotifierEvent.builder()
            .cause(error)
            .context(ctx.getAllValues())
            .notifierId(getNotifierId())
            .notifyType(getType())
            .provider(getProvider())
            .template(template)
            .build());
    }

    protected Mono<Void> onError(String templateId, Values ctx, Throwable error) {
        return onEvent(NotifierEvent.builder()
            .cause(error)
            .context(ctx.getAllValues())
            .notifierId(getNotifierId())
            .notifyType(getType())
            .provider(getProvider())
            .templateId(templateId)
            .build());
    }

    protected Mono<Void> onSuccess(String templateId, Values ctx) {
        return onEvent(NotifierEvent.builder()
            .success(true)
            .context(ctx.getAllValues())
            .notifierId(getNotifierId())
            .notifyType(getType())
            .provider(getProvider())
            .templateId(templateId)
            .build());
    }

    protected Mono<Void> onSuccess(T template, Values ctx) {
        return onEvent(NotifierEvent.builder()
            .success(true)
            .context(ctx.getAllValues())
            .notifierId(getNotifierId())
            .notifyType(getType())
            .provider(getProvider())
            .template(template)
            .build());
    }

    protected abstract Mono<Void> onEvent(NotifierEvent event);

    @Nonnull
    @Override
    public Mono<Void> close() {
        return target.close();
    }
}
