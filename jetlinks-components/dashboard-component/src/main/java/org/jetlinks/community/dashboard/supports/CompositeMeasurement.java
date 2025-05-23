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

import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.MeasurementDefinition;
import org.jetlinks.community.dashboard.MeasurementDimension;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

class CompositeMeasurement implements Measurement {

    private List<Measurement> measurements;

    private Measurement main;

    public CompositeMeasurement(List<Measurement> measurements) {
        Assert.notEmpty(measurements, "measurements can not be empty");
        this.measurements = measurements;
        this.main = measurements.get(0);
    }

    @Override
    public MeasurementDefinition getDefinition() {
        return main.getDefinition();
    }


    @Override
    public Flux<MeasurementDimension> getDimensions() {
        return Flux.fromIterable(measurements)
            .flatMap(Measurement::getDimensions);
    }

    @Override
    public Mono<MeasurementDimension> getDimension(String id) {
        return Flux.fromIterable(measurements)
            .flatMap(measurement -> measurement.getDimension(id))
            .next();
    }
}
