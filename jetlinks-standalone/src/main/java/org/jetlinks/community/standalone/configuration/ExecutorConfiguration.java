package org.jetlinks.community.standalone.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class ExecutorConfiguration {


    @Bean
    public Scheduler reactorScheduler() {
        return Schedulers.parallel();
    }


}
