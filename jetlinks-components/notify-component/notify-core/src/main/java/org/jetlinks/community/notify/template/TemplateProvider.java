package org.jetlinks.community.notify.template;

import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.Provider;
import org.jetlinks.core.metadata.ConfigMetadata;
import reactor.core.publisher.Mono;

public interface TemplateProvider {

    NotifyType getType();

    Provider getProvider();

    Mono<? extends Template> createTemplate(TemplateProperties properties);

    default ConfigMetadata getTemplateConfigMetadata() {
        return null;
    }
}
