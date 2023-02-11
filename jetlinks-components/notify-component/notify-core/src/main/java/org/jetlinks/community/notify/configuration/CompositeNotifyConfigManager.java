package org.jetlinks.community.notify.configuration;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.NotifierProperties;
import org.jetlinks.community.notify.NotifyConfigManager;
import org.jetlinks.community.notify.NotifyType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;

@AllArgsConstructor
public class CompositeNotifyConfigManager implements NotifyConfigManager {
    private final List<NotifyConfigManager> managers;

    @Nonnull
    @Override
    public Mono<NotifierProperties> getNotifyConfig(@Nonnull NotifyType notifyType,
                                                    @Nonnull String configId) {
        Mono<NotifierProperties> mono = null;
        for (NotifyConfigManager manager : managers) {
            if (mono == null) {
                mono = manager.getNotifyConfig(notifyType, configId);
            } else {
                mono = mono.switchIfEmpty(manager.getNotifyConfig(notifyType, configId));
            }
        }
        return mono == null ? Mono.empty() : mono;
    }
}
