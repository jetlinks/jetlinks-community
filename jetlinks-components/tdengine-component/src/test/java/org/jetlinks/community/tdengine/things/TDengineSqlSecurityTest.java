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
package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TDengineSqlSecurityTest {

    @Test
    void shouldUseInternalAliasInRowMode() {
        PropertyAggregation aggregation = new PropertyAggregation(
            "temperature",
            "probe`, current_user as `db_user",
            Aggregation.AVG
        );

        String sql = TDengineRowModeQueryOperations.createAggregationColumn(aggregation, "__agg_0");

        assertEquals("avg(`numberValue`) `__agg_0`", sql);
        assertFalse(sql.contains(aggregation.getAlias()));
    }

    @Test
    void shouldKeepCountValueColumnInRowMode() {
        PropertyAggregation aggregation = new PropertyAggregation(
            "temperature",
            "count",
            Aggregation.COUNT
        );

        String sql = TDengineRowModeQueryOperations.createAggregationColumn(aggregation, "__agg_0");

        assertEquals("count(`value`) `__agg_0`", sql);
    }

    @Test
    void shouldEscapePropertyAndUseInternalAliasInColumnMode() {
        PropertyAggregation aggregation = new PropertyAggregation(
            "temperature`), current_user --",
            "probe`, current_user as `db_user",
            Aggregation.MAX
        );

        String sql = TDengineColumnModeQueryOperations.createAggregationColumn(aggregation, "__agg_1");

        assertEquals("max(`temperature``), current_user --`) `__agg_1`", sql);
        assertFalse(sql.contains(aggregation.getAlias()));
    }

    @Test
    void shouldMapAggregationAliasByPropertyPartition() {
        PropertyAggregation temperatureAvg = new PropertyAggregation(
            "temperature", "temperatureAvg", Aggregation.AVG);
        PropertyAggregation humidityMax = new PropertyAggregation(
            "humidity", "humidityMax", Aggregation.MAX);
        PropertyAggregation temperatureCount = new PropertyAggregation(
            "temperature", "temperatureCount", Aggregation.COUNT);

        TDengineThingDataHelper helper = mock(TDengineThingDataHelper.class);
        when(helper.query(anyString())).thenReturn(Flux.just(
            TimeSeriesData.of(0, Map.of(
                "property", "temperature",
                "__agg_0", 20D,
                "__agg_1", 20D,
                "__agg_2", 2L
            )),
            TimeSeriesData.of(0, Map.of(
                "property", "humidity",
                "__agg_0", 80D,
                "__agg_1", 80D,
                "__agg_2", 3L
            ))
        ));

        AggregationRequest request = AggregationRequest
            .builder()
            .interval(null)
            .limit(1)
            .build();

        List<AggregationData> result = new TestOperations(helper)
            .aggregate(request, temperatureAvg, humidityMax, temperatureCount)
            .collectList()
            .block();

        assertEquals(1, result.size(), String.valueOf(result));
        assertEquals(
            Map.of(
                "temperatureAvg", 20D,
                "humidityMax", 80D,
                "temperatureCount", 2L
            ),
            result.get(0).asMap()
        );
    }

    private static class TestOperations extends TDengineRowModeQueryOperations {

        TestOperations(TDengineThingDataHelper helper) {
            super("device", "product", null,
                  MetricBuilder.DEFAULT, new DataSettings(), null, helper);
        }

        Flux<AggregationData> aggregate(AggregationRequest request,
                                        PropertyAggregation... properties) {
            return doAggregation(
                "device_properties_product",
                request,
                new AggregationContext(mock(ThingMetadata.class), properties)
            );
        }
    }
}
