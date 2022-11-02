package org.jetlinks.community.timeseries;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class TimeSeriesManagerConfiguration {

    @ConditionalOnMissingBean(TimeSeriesManager.class)
    @Bean
    public NoneTimeSeriesManager timeSeriesManager() {
        return new NoneTimeSeriesManager();
    }
}
