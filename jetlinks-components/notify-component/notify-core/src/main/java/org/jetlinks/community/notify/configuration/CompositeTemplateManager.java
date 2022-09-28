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
