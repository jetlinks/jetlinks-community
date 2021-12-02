package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.event.BrokerEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class RuleConfiguration {
    @Bean
    public EventBus eventBus() {
        return new BrokerEventBus();
    }

    @Bean
    public Scheduler reactorScheduler() {
        return Schedulers.parallel();
    }
}
