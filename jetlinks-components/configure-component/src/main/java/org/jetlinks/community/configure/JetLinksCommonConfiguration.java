package org.jetlinks.community.configure;

import org.jetlinks.community.configure.compatible.PathCompatibleFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class JetLinksCommonConfiguration {

    @Bean
    public Scheduler reactorScheduler() {
        return Schedulers.parallel();
    }

    @Bean
    public PathCompatibleFilter pathCompatibleFilter(){
        return new PathCompatibleFilter();
    }
}
