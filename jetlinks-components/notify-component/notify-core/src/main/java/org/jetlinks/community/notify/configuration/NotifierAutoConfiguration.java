package org.jetlinks.community.notify.configuration;

import lombok.Generated;
import org.jetlinks.community.notify.*;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.template.TemplateManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StaticNotifyProperties.class)
@Generated
public class NotifierAutoConfiguration {

    @Bean
    public StaticNotifyConfigManager staticNotifyConfigManager(StaticNotifyProperties properties) {
        return new StaticNotifyConfigManager(properties);
    }

    @Bean
    @Primary
    public NotifyConfigManager notifyConfigManager(List<NotifyConfigManager> managers) {
        return new CompositeNotifyConfigManager(managers);
    }

    @Bean
    public StaticTemplateManager staticTemplateManager(StaticNotifyProperties properties) {
        return new StaticTemplateManager(properties);
    }

    @Bean
    @Primary
    public TemplateManager templateManager(List<TemplateManager> managers) {
        return new CompositeTemplateManager(managers);
    }


    @Bean
    @ConditionalOnMissingBean(NotifierManager.class)
    public DefaultNotifierManager notifierManager(EventBus eventBus,
                                                  NotifyConfigManager configManager) {
        return new DefaultNotifierManager(eventBus, configManager);
    }

}
