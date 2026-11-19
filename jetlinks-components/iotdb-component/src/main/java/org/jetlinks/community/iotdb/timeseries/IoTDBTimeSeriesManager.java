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

import org.jetlinks.community.iotdb.IoTDBOperations;
import org.jetlinks.community.iotdb.SchemaColumn;
import org.jetlinks.community.iotdb.utils.IoTDBSqlUtils;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.FloatType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.GeoType;
import org.jetlinks.community.things.data.ThingsDataConstants;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IoTDBTimeSeriesManager implements TimeSeriesManager {

    private final IoTDBOperations operations;
    private final ConcurrentMap<String, TimeSeriesService> services = new ConcurrentHashMap<>();

    public IoTDBTimeSeriesManager(IoTDBOperations operations) {
        this.operations = operations;
    }

    @Override
    public TimeSeriesService getService(TimeSeriesMetric metric) {
        return getService(metric.getId());
    }

    @Override
    public TimeSeriesService getServices(TimeSeriesMetric... metrics) {
        if (metrics == null || metrics.length != 1) {
            throw new UnsupportedOperationException("IoTDB table model supports one metric per service");
        }
        return getService(metrics[0]);
    }

    @Override
    public TimeSeriesService getServices(String... metrics) {
        if (metrics == null || metrics.length != 1) {
            throw new UnsupportedOperationException("IoTDB table model supports one metric per service");
        }
        return getService(metrics[0]);
    }

    @Override
    public TimeSeriesService getService(String metric) {
        if (metric == null || metric.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric must not be empty");
        }
        return services.computeIfAbsent(metric, key -> new IoTDBTimeSeriesService(key, operations));
    }

    @Override
    public Mono<Void> registerMetadata(TimeSeriesMetadata metadata) {
        if (metadata == null || metadata.getMetric() == null) {
            return Mono.error(new IllegalArgumentException("Time-series metric metadata is required"));
        }
        List<SchemaColumn> columns = new ArrayList<>();
        columns.add(new SchemaColumn(ThingsDataConstants.COLUMN_ID, "STRING", true));
        for (PropertyMetadata property : metadata.getProperties()) {
            if (property == null || property.getId() == null || property.getId().isEmpty()) {
                continue;
            }
            if (ThingsDataConstants.COLUMN_TIMESTAMP.equalsIgnoreCase(property.getId())
                || ThingsDataConstants.COLUMN_ID.equalsIgnoreCase(property.getId())
                || "time".equalsIgnoreCase(property.getId())) {
                continue;
            }
            columns.add(new SchemaColumn(property.getId(), toIoTDBType(property.getValueType()), false));
        }
        return operations.ensureTable(metadata.getMetric().getId(), columns);
    }

    static String toIoTDBType(DataType type) {
        if (type == null) {
            return "STRING";
        }
        if (type instanceof BooleanType) {
            return "BOOLEAN";
        }
        if (type instanceof IntType) {
            return "INT32";
        }
        if (type instanceof DateTimeType) {
            return "TIMESTAMP";
        }
        if (type instanceof LongType) {
            return "INT64";
        }
        if (type instanceof FloatType) {
            return "FLOAT";
        }
        if (type instanceof DoubleType || type instanceof NumberType) {
            return "DOUBLE";
        }
        if (type instanceof ArrayType || type instanceof ObjectType || type instanceof GeoType) {
            return "STRING";
        }
        return "STRING";
    }
}
