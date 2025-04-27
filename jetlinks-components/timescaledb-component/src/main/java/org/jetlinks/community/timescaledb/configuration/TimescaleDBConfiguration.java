package org.jetlinks.community.timescaledb.configuration;

import org.jetlinks.community.timescaledb.TimescaleDBProperties;
import org.jetlinks.community.timescaledb.impl.DefaultTimescaleDBOperations;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(TimescaleDBProperties.class)
@ConditionalOnProperty(prefix = "timescaledb", name = "enabled", havingValue = "true")
public class TimescaleDBConfiguration {


    @Bean(destroyMethod = "shutdown",initMethod = "init")
    public DefaultTimescaleDBOperations timescaleDBOperations(TimescaleDBProperties properties) {

        return new DefaultTimescaleDBOperations(properties);
    }


}
