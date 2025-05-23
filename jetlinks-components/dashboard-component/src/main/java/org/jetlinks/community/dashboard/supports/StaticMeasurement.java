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
import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.MeasurementDefinition;
import org.jetlinks.community.dashboard.MeasurementDimension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaticMeasurement implements Measurement {

    @Getter
    private MeasurementDefinition definition;


    public StaticMeasurement(MeasurementDefinition definition) {
        this.definition = definition;
    }

    private Map<String, MeasurementDimension> dimensions = new ConcurrentHashMap<>();

    public StaticMeasurement addDimension(MeasurementDimension dimension) {

        dimensions.put(dimension.getDefinition().getId(), dimension);

        return this;

    }

    @Override
    public Flux<MeasurementDimension> getDimensions() {
        return Flux.fromIterable(dimensions.values());
    }

    @Override
    public Mono<MeasurementDimension> getDimension(String id) {
        return Mono.justOrEmpty(dimensions.get(id));
    }
}
