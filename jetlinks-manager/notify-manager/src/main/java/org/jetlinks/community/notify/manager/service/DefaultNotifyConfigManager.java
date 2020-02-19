package org.jetlinks.community.notify.manager.service;

import org.jetlinks.community.notify.NotifierProperties;
import org.jetlinks.community.notify.NotifyConfigManager;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Service
public class DefaultNotifyConfigManager implements NotifyConfigManager {

    @Autowired
    private NotifyConfigService configService;

    @Nonnull
    @Override
    public Mono<NotifierProperties> getNotifyConfig(@Nonnull NotifyType notifyType, @Nonnull String configId) {
        return configService.findById(configId)
                .map(NotifyConfigEntity::toProperties);
    }
}
