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
package org.jetlinks.community.iotdb.things;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.Interval;
import org.jetlinks.community.iotdb.IoTDBClientOperations;
import org.jetlinks.community.iotdb.IoTDBProperties;
import org.jetlinks.community.iotdb.SchemaColumn;
import org.jetlinks.community.iotdb.timeseries.IoTDBTimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.operations.AbstractDDLOperations;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IoTDBNativeIntegrationTest {

    @Test
    void shouldRoundTripNativeTableAndGenericTimeSeries() {
        IoTDBClientOperations operations = createOperations();
        try {
            String metric = "NativeMetric_" + suffix();
            List<SchemaColumn> schema = new ArrayList<>(List.of(
                new SchemaColumn("id", "STRING", true),
                new SchemaColumn("deviceId", "STRING", true),
                new SchemaColumn("temperature", "DOUBLE", false),
                new SchemaColumn("status", "STRING", false),
                new SchemaColumn("enabled", "BOOLEAN", false),
                new SchemaColumn("payload", "STRING", false)));

            operations.ensureTable(metric, schema).block(Duration.ofSeconds(90));
            schema.add(new SchemaColumn("battery", "INT32", false));
            operations.ensureTable(metric, schema).block(Duration.ofSeconds(90));

            long timestamp = 1_700_000_000_000L;
            List<TimeSeriesData> rows = List.of(
                row(timestamp, "id-1", "device-1", 21.5d, "ready", true, Map.of("nested", 1), 87),
                row(timestamp, "id-2", "device-2", 25.0d, null, false, List.of("a", 2), 88),
                row(timestamp + 60_000, "id-3", "device-1", 24.0d, "warm", true, Map.of("nested", 3), 89));
            operations.write(metric, Flux.fromIterable(rows), Set.of("id", "deviceId"))
                .block(Duration.ofSeconds(90));

            List<Map<String, Object>> selected = operations.query(
                "SELECT *, time AS _ts FROM " + table(operations, metric) + " ORDER BY time DESC")
                .collectList().block(Duration.ofSeconds(30));
            assertNotNull(selected);
            assertEquals(3, selected.size(), "tag values plus time must retain equal-time rows");
            assertTrue(selected.stream().anyMatch(row -> "id-2".equals(row.get("id")) && row.get("status") == null));
            assertEquals("{\"nested\":1}", selected.stream()
                .filter(row -> "id-1".equals(row.get("id")))
                .findFirst().orElseThrow().get("payload"));
            assertEquals(3d, number(operations.query("SELECT COUNT(*) AS total FROM " + table(operations, metric))
                .single().block().get("total")));

            String genericMetric = "GenericMetric_" + suffix();
            IoTDBTimeSeriesManager manager = new IoTDBTimeSeriesManager(operations);
            manager.registerMetadata(TimeSeriesMetadata.of(
                TimeSeriesMetric.of(genericMetric),
                SimplePropertyMetadata.of("deviceId", "Device", StringType.GLOBAL),
                SimplePropertyMetadata.of("temperature", "Temperature", DoubleType.GLOBAL),
                SimplePropertyMetadata.of("status", "Status", StringType.GLOBAL),
                SimplePropertyMetadata.of("enabled", "Enabled", BooleanType.GLOBAL),
                SimplePropertyMetadata.of("payload", "Payload", StringType.GLOBAL),
                SimplePropertyMetadata.of("battery", "Battery", IntType.GLOBAL))).block(Duration.ofSeconds(90));
            TimeSeriesService service = manager.getService(genericMetric);
            service.save(Flux.fromIterable(rows)).block(Duration.ofSeconds(90));

            QueryParamEntity filter = QueryParamEntity.of();
            Term temperature = new Term();
            temperature.setColumn("temperature");
            temperature.setTermType("gte");
            temperature.setValue(23d);
            filter.setTerms(List.of(temperature));
            filter.setPaging(true);
            filter.setPageSize(10);
            filter.setPageIndex(0);
            assertEquals(2, service.count(filter).block(Duration.ofSeconds(30)));
            assertEquals(2, service.query(filter).collectList().block(Duration.ofSeconds(30)).size());

            AggregationQueryParam sum = AggregationQueryParam.of().sum("temperature", "total")
                .from(timestamp).to(timestamp + 120_000);
            assertEquals(70.5d, number(service.aggregation(sum).single().block(Duration.ofSeconds(30))
                .get("total").orElseThrow()));

            AggregationQueryParam bucketed = AggregationQueryParam.of().sum("temperature", "total")
                .from(timestamp).to(timestamp + 120_000)
                .groupBy(Interval.ofMinutes(1), "window", "yyyy-MM-dd HH:mm");
            List<?> buckets = service.aggregation(bucketed).collectList().block(Duration.ofSeconds(30));
            assertNotNull(buckets);
            assertEquals(2, buckets.size());
            assertFalse(buckets.isEmpty());
        } finally {
            cleanUpTestDatabase(operations);
        }
    }

    @Test
    void shouldRoundTripThingPropertyRowsAndColumns() {
        IoTDBClientOperations operations = createOperations();
        try {
            IoTDBThingDataHelper helper = new IoTDBThingDataHelper(operations);
            List<PropertyMetadata> rowProperties = List.of(
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_ID, "ID", StringType.GLOBAL),
                SimplePropertyMetadata.of("thingId", "Device", StringType.GLOBAL),
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_ID, "Property", StringType.GLOBAL),
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_NUMBER_VALUE, "Number", DoubleType.GLOBAL),
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_TIMESTAMP, "Timestamp", DateTimeType.GLOBAL));
            String rowMetric = "ThingRow_" + suffix();
            helper.registerMetric(AbstractDDLOperations.MetricType.properties, rowMetric, rowProperties, true, "thingId")
                .block(Duration.ofSeconds(90));

            long timestamp = 1_700_000_000_000L;
            Map<String, Object> row1 = Map.of("id", "row-1", "thingId", "device-1", "property", "temperature", "numberValue", 20.5d);
            Map<String, Object> row2 = Map.of("id", "row-2", "thingId", "device-1", "property", "humidity", "numberValue", 42.0d);
            helper.doSave(rowMetric, Flux.just(TimeSeriesData.of(timestamp, row1), TimeSeriesData.of(timestamp, row2)))
                .block(Duration.ofSeconds(90));
            List<TimeSeriesData> propertyRows = helper.doQuery(rowMetric, QueryParamEntity.of())
                .collectList().block(Duration.ofSeconds(30));
            assertNotNull(propertyRows);
            assertEquals(2, propertyRows.size());

            List<PropertyMetadata> columnProperties = List.of(
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_ID, "ID", StringType.GLOBAL),
                SimplePropertyMetadata.of("thingId", "Device", StringType.GLOBAL),
                SimplePropertyMetadata.of("temperature", "Temperature", DoubleType.GLOBAL),
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_TIMESTAMP, "Timestamp", DateTimeType.GLOBAL));
            String columnMetric = "ThingColumn_" + suffix();
            helper.registerMetric(AbstractDDLOperations.MetricType.properties, columnMetric, columnProperties, false, "thingId")
                .block(Duration.ofSeconds(90));
            Map<String, Object> columnRow = Map.of("id", "column-1", "thingId", "device-1", "temperature", 26.5d);
            helper.doSave(columnMetric, TimeSeriesData.of(timestamp, columnRow)).block(Duration.ofSeconds(90));
            List<TimeSeriesData> columnRows = helper.doQuery(columnMetric, QueryParamEntity.of())
                .collectList().block(Duration.ofSeconds(30));
            assertNotNull(columnRows);
            assertEquals(1, columnRows.size());
            assertEquals(26.5d, number(columnRows.get(0).getData().get("temperature")));
        } finally {
            cleanUpTestDatabase(operations);
        }
    }

    private static void cleanUpTestDatabase(IoTDBClientOperations operations) {
        try {
            operations.execute("DROP DATABASE IF EXISTS " + operations.getDatabase()).block(Duration.ofSeconds(30));
        } finally {
            operations.dispose();
        }
    }

    private static IoTDBClientOperations createOperations() {
        String endpoint = System.getenv("IOTDB_TEST_ENDPOINT");
        String username = System.getenv("IOTDB_TEST_USERNAME");
        String password = System.getenv("IOTDB_TEST_PASSWORD");
        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(), "IOTDB_TEST_ENDPOINT is not configured");
        Assumptions.assumeTrue(username != null && !username.isBlank(), "IOTDB_TEST_USERNAME is not configured");
        Assumptions.assumeTrue(password != null && !password.isBlank(), "IOTDB_TEST_PASSWORD is not configured");

        IoTDBProperties properties = new IoTDBProperties();
        properties.setDatabase("db_eco_16_" + suffix().toLowerCase());
        properties.setNodeUrls(List.of(endpoint));
        properties.setUsername(username);
        properties.setPassword(password);
        properties.setMaxSize(4);
        properties.setQueryTimeoutInMs(30_000);
        properties.setFetchSize(128);
        properties.setConnectionTimeoutInMs(5_000);
        properties.setWriteBatchSize(2);
        return new IoTDBClientOperations(properties);
    }

    private static TimeSeriesData row(long timestamp,
                                      String id,
                                      String device,
                                      double temperature,
                                      String status,
                                      boolean enabled,
                                      Object payload,
                                      int battery) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("deviceId", device);
        values.put("temperature", temperature);
        values.put("status", status);
        values.put("enabled", enabled);
        values.put("payload", payload);
        values.put("battery", battery);
        return TimeSeriesData.of(timestamp, values);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String table(IoTDBClientOperations operations, String metric) {
        return "\"" + operations.getDatabase() + "\".\""
            + org.jetlinks.community.iotdb.utils.IoTDBSqlUtils.encodeMetricName(metric) + "\"";
    }

    private static double number(Object value) {
        return ((Number) value).doubleValue();
    }
}
