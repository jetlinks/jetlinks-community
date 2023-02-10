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
