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
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.micrometer.MeterRegistrySupplier;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TimeSeriesMeterRegistrySupplier implements MeterRegistrySupplier {

    private final TimeSeriesManager timeSeriesManager;

    @Getter
    @Setter
    private String naming = "elastic";

    static Map<String, NamingConvention> namingSupports = new HashMap<>();

    static {
        namingSupports.put("elastic", new ElasticNamingConvention());
    }

    @Getter
    @Setter
    private Map<String, String> tags = new HashMap<>();

    @Getter
    @Setter
    private Set<String> ignore = new HashSet<>();

    @Getter
    @Setter
    private Map<String, TimeSeriesRegistryProperties> metrics = new HashMap<>();

    public TimeSeriesMeterRegistrySupplier(TimeSeriesManager timeSeriesManager) {
        this.timeSeriesManager = timeSeriesManager;
        metrics.put("default", new TimeSeriesRegistryProperties());
    }

    @Override
    public MeterRegistry getMeterRegistry(String metric, String... tagKeys) {
        if (ignore.contains(metric)) {
            return new SimpleMeterRegistry();
        }
        TimeSeriesMeterRegistry registry = new TimeSeriesMeterRegistry(timeSeriesManager,
            TimeSeriesMetric.of(metric),
            metrics.getOrDefault(metric, metrics.get("default")),
            tags, tagKeys);
        registry.config().namingConvention(namingSupports.get(naming));
        return registry;
    }

}
