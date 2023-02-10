package org.jetlinks.community.notify.template;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.community.notify.NotifyType;
import org.springframework.boot.CommandLineRunner;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractTemplateManager implements TemplateManager {

    protected final Map<String, Map<String, TemplateProvider>> providers = new ConcurrentHashMap<>();

    protected final Map<String, Template> templates = new ConcurrentHashMap<>();

    protected abstract Mono<TemplateProperties> getProperties(NotifyType type, String id);

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
        return Mono.justOrEmpty(templates.get(id))
                   .switchIfEmpty(Mono.defer(() -> this
                       .getProperties(type, id)
                       .flatMap(prop -> this.createTemplate(type, prop))
                       .doOnNext(template -> templates.put(id, template))
                       .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("通知类型不支持:" + type.getId())))
                   ));
    }

    @Override
    @Nonnull
    public Mono<Void> reload(String templateId) {
        return doReload(templateId)
            .then();
    }

    private Mono<String> doReload(String templateId) {
        log.debug("reload notify template {}",templateId);
        return Mono.justOrEmpty(templates.remove(templateId))
                   .thenReturn(templateId);
    }


}
