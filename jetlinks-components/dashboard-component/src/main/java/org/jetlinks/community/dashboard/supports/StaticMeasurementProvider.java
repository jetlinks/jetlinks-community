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
package org.jetlinks.community.dashboard.supports;

import lombok.Getter;
import org.jetlinks.community.dashboard.DashboardDefinition;
import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.ObjectDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StaticMeasurementProvider implements MeasurementProvider {

    private Map<String, Measurement> measurements = new ConcurrentHashMap<>();

    @Getter
    private DashboardDefinition dashboardDefinition;
    @Getter
    private ObjectDefinition objectDefinition;

    public StaticMeasurementProvider(DashboardDefinition dashboardDefinition,
                                     ObjectDefinition objectDefinition) {
        this.dashboardDefinition = dashboardDefinition;
        this.objectDefinition = objectDefinition;
    }

    protected void addMeasurement(Measurement measurement) {
        measurements.put(measurement.getDefinition().getId(), measurement);
    }

    @Override
    public Flux<Measurement> getMeasurements() {
        return Flux.fromIterable(measurements.values());
    }

    @Override
    public Mono<Measurement> getMeasurement(String id) {
        return Mono.justOrEmpty(measurements.get(id));
    }
}
