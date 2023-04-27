package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeSeriesMeterRegistryConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "micrometer.time-series")
    public TimeSeriesMeterRegistrySupplier timeSeriesMeterRegistrySupplier(TimeSeriesManager timeSeriesManager) {
        return new TimeSeriesMeterRegistrySupplier(timeSeriesManager);
    }

    @Bean
    @ConditionalOnEnabledMetricsExport("simple")
    public MeterRegistry meterRegistry(TimeSeriesMeterRegistrySupplier registrySupplier) {
        // TODO: 2020/2/13 配置化
        return registrySupplier.getMeterRegistry("jetlinks-metrics");
    }


}
