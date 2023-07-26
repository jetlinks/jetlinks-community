package org.jetlinks.community.notify.manager.configuration;

import org.jetlinks.community.notify.manager.service.InDatabaseNotifyHistoryRepository;
import org.jetlinks.community.notify.manager.service.NotifyHistoryRepository;
import org.jetlinks.community.notify.manager.service.NotifyHistoryService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@EnableConfigurationProperties(NotifySubscriberProperties.class)
public class NotifyConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotifyHistoryRepository.class)
    public NotifyHistoryRepository notifyHistoryRepository(NotifyHistoryService historyService) {
        return new InDatabaseNotifyHistoryRepository(historyService);
    }
}
