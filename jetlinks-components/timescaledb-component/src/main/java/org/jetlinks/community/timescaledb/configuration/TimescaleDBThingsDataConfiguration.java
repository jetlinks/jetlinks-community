package org.jetlinks.community.timescaledb.configuration;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.jetlinks.community.timescaledb.TimescaleDBOperations;
import org.jetlinks.community.timescaledb.thing.TimescaleDBColumnModeStrategy;
import org.jetlinks.community.timescaledb.thing.TimescaleDBRowModeStrategy;
import org.jetlinks.community.timescaledb.thing.TimescaleDBThingsDataProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = TimescaleDBConfiguration.class)
@ConditionalOnBean({TimescaleDBOperations.class, ThingsRegistry.class})
@ConditionalOnClass(ThingsDataRepositoryStrategy.class)
@ConditionalOnProperty(prefix = "timescaledb.things-data", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TimescaleDBThingsDataProperties.class)
public class TimescaleDBThingsDataConfiguration {


    @Bean
    public TimescaleDBRowModeStrategy timescaleDBRowModeStrategy(ThingsRegistry registry,
                                                                 TimescaleDBOperations timescaleDBOperations,
                                                                 TimescaleDBThingsDataProperties properties) {
        return new TimescaleDBRowModeStrategy(registry, timescaleDBOperations, properties);
    }

    @Bean
    public TimescaleDBColumnModeStrategy timescaleDBColumnModeStrategy(ThingsRegistry registry,
                                                                       TimescaleDBOperations timescaleDBOperations,
                                                                       TimescaleDBThingsDataProperties properties) {
        return new TimescaleDBColumnModeStrategy(registry, timescaleDBOperations, properties);
    }


}
