package org.jetlinks.community.notify;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.configuration.StaticNotifyProperties;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@AllArgsConstructor
public class StaticNotifyConfigManager implements NotifyConfigManager{

    private StaticNotifyProperties properties;

    @Nonnull
    @Override
    public Mono<NotifierProperties> getNotifyConfig(@Nonnull NotifyType notifyType, @Nonnull String configId) {
        return Mono.justOrEmpty(properties.getNotifierProperties(notifyType,configId));
    }
}
