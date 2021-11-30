package org.jetlinks.community.logging.configuration;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.event.BrokerEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogConfiguration {
    @Bean
    public EventBus eventBus() {
        return new BrokerEventBus();
    }
}
