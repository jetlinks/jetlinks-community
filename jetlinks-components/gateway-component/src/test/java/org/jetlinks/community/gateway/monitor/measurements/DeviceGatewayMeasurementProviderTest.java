/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.gateway.monitor.measurements;

import org.jetlinks.community.dashboard.CommonDimensionDefinition;
import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class DeviceGatewayMeasurementProviderTest {

    @Test
    void shouldQueryHistoryAndAggregationMeasurements() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);

        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(timeSeriesService);

        Map<String, Object> value = new HashMap<>();
        value.put("value", 100);
        value.put("time", "2020-01-10");
        Mockito.when(timeSeriesService.query(Mockito.any()))
            .thenReturn(Flux.just(TimeSeriesData.of(System.currentTimeMillis(), value)));
        Mockito.when(timeSeriesService.aggregation(Mockito.any()))
            .thenReturn(Flux.just(AggregationData.of(value)));

        DeviceGatewayMeasurementProvider provider = new DeviceGatewayMeasurementProvider(timeSeriesManager);

        provider
            .getMeasurement("connection")
            .flatMapMany(measurement -> measurement
                .getDimension(CommonDimensionDefinition.history.getId())
                .flatMapMany(dimension -> dimension.getValue(MeasurementParameter.of(Collections.emptyMap()))))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        provider
            .getMeasurement("connection")
            .flatMapMany(measurement -> measurement
                .getDimension(CommonDimensionDefinition.agg.getId())
                .flatMapMany(dimension -> dimension.getValue(MeasurementParameter.of(Collections.emptyMap()))))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }
}
