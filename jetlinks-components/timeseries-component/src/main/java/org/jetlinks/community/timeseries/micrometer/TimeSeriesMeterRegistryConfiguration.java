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
