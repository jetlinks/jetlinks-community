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
package org.jetlinks.community.iotdb.timeseries;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.iotdb.IoTDBOperations;
import org.jetlinks.community.iotdb.SchemaColumn;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.StringType;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IoTDBTimeSeriesServiceTest {

    @Test
    void shouldRegisterMetricSchemaWithTypedFieldsAndIdentityTag() {
        IoTDBOperations operations = mock(IoTDBOperations.class);
        when(operations.ensureTable(anyString(), anyList())).thenReturn(Mono.empty());
        IoTDBTimeSeriesManager manager = new IoTDBTimeSeriesManager(operations);
        TimeSeriesMetadata metadata = TimeSeriesMetadata.of(
            TimeSeriesMetric.of("plant.Temperature"),
            SimplePropertyMetadata.of("temperature", "Temperature", DoubleType.GLOBAL),
            SimplePropertyMetadata.of("status", "Status", StringType.GLOBAL),
            SimplePropertyMetadata.of("createdAt", "Created at", DateTimeType.GLOBAL),
            SimplePropertyMetadata.of("timestamp", "Timestamp", DoubleType.GLOBAL));

        manager.registerMetadata(metadata).block();

        verify(operations).ensureTable(org.mockito.ArgumentMatchers.eq("plant.Temperature"), anyList());
        org.mockito.ArgumentCaptor<List<SchemaColumn>> columns = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(operations).ensureTable(org.mockito.ArgumentMatchers.eq("plant.Temperature"), columns.capture());
        assertEquals(List.of("id", "temperature", "status", "createdAt"),
                     columns.getValue().stream().map(SchemaColumn::getName).toList());
        assertTrue(columns.getValue().get(0).isTag());
        assertEquals("DOUBLE", columns.getValue().get(1).getType());
        assertEquals("STRING", columns.getValue().get(2).getType());
        assertEquals("TIMESTAMP", columns.getValue().get(3).getType());
    }

    @Test
    void shouldWriteIdentityAndUseIdAsTag() {
        IoTDBOperations operations = mock(IoTDBOperations.class);
        AtomicReference<Publisher<TimeSeriesData>> written = new AtomicReference<>();
        AtomicReference<Set<String>> tags = new AtomicReference<>();
        when(operations.write(anyString(), any(), anySet())).thenAnswer(invocation -> {
            written.set(invocation.getArgument(1));
            tags.set(invocation.getArgument(2));
            return Mono.empty();
        });
        IoTDBTimeSeriesService service = new IoTDBTimeSeriesService("metric", operations);
        Map<String, Object> values = new HashMap<>();
        values.put("temperature", 22.5d);
        long timestamp = 1_700_000_000_000L;

        service.save(Flux.just(TimeSeriesData.of(timestamp, values))).block();

        TimeSeriesData data = Flux.from(written.get()).single().block();
        assertNotNull(data.getData().get("id"));
        assertEquals(timestamp, data.getData().get("timestamp"));
        assertEquals(22.5d, data.getData().get("temperature"));
        assertEquals(Set.of("id"), tags.get());
    }

    @Test
    void shouldQueryAndAggregateUsingEncodedNames() {
        IoTDBOperations operations = mock(IoTDBOperations.class);
        when(operations.getDatabase()).thenReturn("jetlinks");
        when(operations.query(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("COUNT(*)")) {
                return Flux.just(Map.of("total", 1L));
            }
            if (sql.contains("SUM(")) {
                return Flux.just(Map.of("iotdb_agg_0", 42.0d));
            }
            return Flux.just(Map.of("_ts", new Date(1_700_000_000_000L),
                                    "c_74656d7065726174757265", 21.5d));
        });
        IoTDBTimeSeriesService service = new IoTDBTimeSeriesService("metric", operations);

        TimeSeriesData row = service.query(QueryParamEntity.of()).single().block();
        assertEquals(1_700_000_000_000L, row.getTimestamp());
        assertEquals(1_700_000_000_000L, row.getData().get("timestamp"));
        assertEquals(21.5d, row.getData().get("temperature"));

        AggregationData aggregate = service.aggregation(AggregationQueryParam.of().sum("temperature", "total"))
            .single().block();
        assertEquals(42.0d, aggregate.get("total").orElse(null));
        verify(operations, atLeastOnce()).query(org.mockito.ArgumentMatchers.contains("\"jetlinks\".\"m_6d6574726963\""));
        verify(operations).query(org.mockito.ArgumentMatchers.contains("\"c_74656d7065726174757265\""));
    }
}
