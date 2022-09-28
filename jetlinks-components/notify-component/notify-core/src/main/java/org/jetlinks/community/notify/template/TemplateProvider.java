package org.jetlinks.community.notify.template;

import org.jetlinks.community.notify.Provider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.notify.NotifyType;
import reactor.core.publisher.Mono;

public interface TemplateProvider {

    NotifyType getType();

    Provider getProvider();

    Mono<? extends Template> createTemplate(TemplateProperties properties);

    default ConfigMetadata getTemplateConfigMetadata() {
        return null;
    }
}
