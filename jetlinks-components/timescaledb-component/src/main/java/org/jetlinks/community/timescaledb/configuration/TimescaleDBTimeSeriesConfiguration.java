package org.jetlinks.community.timescaledb.configuration;

import org.jetlinks.community.timescaledb.TimescaleDBOperations;
import org.jetlinks.community.timescaledb.timeseries.TimescaleDBTimeSeriesManager;
import org.jetlinks.community.timescaledb.timeseries.TimescaleDBTimeSeriesProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@AutoConfiguration(after = TimescaleDBConfiguration.class)
@ConditionalOnBean(TimescaleDBOperations.class)
@ConditionalOnProperty(prefix = "timescaledb.time-series", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TimescaleDBTimeSeriesProperties.class)
public class TimescaleDBTimeSeriesConfiguration {


    @Bean
    @Primary
    public TimescaleDBTimeSeriesManager timescaleDBTimeSeriesManager(TimescaleDBOperations operations,
                                                                     TimescaleDBTimeSeriesProperties properties) {
        return new TimescaleDBTimeSeriesManager(properties, operations);
    }


}
