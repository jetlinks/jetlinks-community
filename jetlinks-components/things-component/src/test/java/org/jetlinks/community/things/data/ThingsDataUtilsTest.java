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
package org.jetlinks.community.things.data;

import org.jetlinks.community.Interval;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ThingsDataUtilsTest {

    private static final PropertyAggregation TEMPERATURE =
        new PropertyAggregation("temperature", "temperatureAvg", Aggregation.AVG);

    @Test
    void shouldMatchNonZeroTimestampWithoutInterval() {
        AggregationRequest request = AggregationRequest
            .builder()
            .interval(null)
            .build();

        NavigableMap<Long, Map<String, Object>> prepares =
            ThingsDataUtils.prepareAggregationData(request, TEMPERATURE);

        assertEquals(0L, prepares.firstKey());
        assertSame(prepares.get(0L), ThingsDataUtils.findAggregationData(1_725_000_000_000L, prepares));
    }

    @Test
    void shouldMatchRawTimestampToPreviousTimeBucket() {
        long from = 1_725_000_000_000L;
        AggregationRequest request = AggregationRequest
            .builder()
            .interval(Interval.ofHours(1))
            .format("yyyy-MM-dd HH:mm")
            .from(new Date(from))
            .to(new Date(from + 2 * 60 * 60 * 1000L))
            .build();

        NavigableMap<Long, Map<String, Object>> prepares =
            ThingsDataUtils.prepareAggregationData(request, (time, interval) -> time, TEMPERATURE);

        long firstBucket = prepares.firstKey();
        assertSame(
            prepares.get(firstBucket),
            ThingsDataUtils.findAggregationData(firstBucket + 30 * 60 * 1000L, prepares)
        );
    }

    @Test
    void shouldPrepareNaturalOrderAndReadNewestFirst() {
        long from = 1_725_000_000_000L;
        AggregationRequest request = AggregationRequest
            .builder()
            .interval(Interval.ofHours(1))
            .format("yyyy-MM-dd HH:mm")
            .from(new Date(from))
            .to(new Date(from + 2 * 60 * 60 * 1000L))
            .build();

        NavigableMap<Long, Map<String, Object>> prepares =
            ThingsDataUtils.prepareAggregationData(request, (time, interval) -> time, TEMPERATURE);

        assertEquals(prepares.lastKey(), prepares.descendingMap().firstKey());
    }
}
