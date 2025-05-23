/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
