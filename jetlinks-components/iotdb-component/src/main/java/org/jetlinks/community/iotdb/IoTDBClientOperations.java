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
package org.jetlinks.community.iotdb;

import org.apache.iotdb.isession.ITableSession;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.isession.pool.ITableSessionPool;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.tsfile.enums.ColumnCategory;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.read.common.Field;
import org.apache.tsfile.read.common.RowRecord;
import org.apache.tsfile.write.record.Tablet;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.community.iotdb.utils.IoTDBSqlUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class IoTDBClientOperations implements IoTDBOperations {

    private static final Set<TSDataType> TABLE_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        TSDataType.BOOLEAN,
        TSDataType.INT32,
        TSDataType.INT64,
        TSDataType.FLOAT,
        TSDataType.DOUBLE,
        TSDataType.STRING,
        TSDataType.TEXT,
        TSDataType.TIMESTAMP,
        TSDataType.DATE,
        TSDataType.BLOB
    )));

    private final String database;
    private final ITableSessionPool pool;
    private final long queryTimeoutInMs;
    private final int writeBatchSize;
    private final AtomicBoolean disposed = new AtomicBoolean();
    private final ConcurrentMap<String, List<ColumnSpec>> schemas = new ConcurrentHashMap<>();

    public IoTDBClientOperations(IoTDBProperties properties) {
        this(Objects.requireNonNull(properties, "properties").getDatabase(), createPool(properties),
             properties.getQueryTimeoutInMs(), properties.getWriteBatchSize());
    }

    public IoTDBClientOperations(String database,
                                 ITableSessionPool pool,
                                 long queryTimeoutInMs,
                                 int writeBatchSize) {
        if (database == null || database.trim().isEmpty()) {
            throw new IllegalArgumentException("IoTDB database must not be empty");
        }
        if (queryTimeoutInMs < 1 || writeBatchSize < 1) {
            throw new IllegalArgumentException("IoTDB timeout and write batch size must be positive");
        }
        this.database = database;
        this.pool = Objects.requireNonNull(pool, "pool");
        this.queryTimeoutInMs = queryTimeoutInMs;
        this.writeBatchSize = writeBatchSize;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public Mono<Void> execute(String sql) {
        Objects.requireNonNull(sql, "sql");
        return Mono.<Void>fromRunnable(() -> withSession(session -> {
            session.executeNonQueryStatement(sql);
            return null;
        })).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Map<String, Object>> query(String sql) {
        Objects.requireNonNull(sql, "sql");
        return Flux.usingWhen(
            acquireSession(),
            session -> Flux.using(
                    () -> session.executeQueryStatement(sql, queryTimeoutInMs),
                    this::rows,
                    IoTDBClientOperations::closeDataSet)
                .subscribeOn(Schedulers.boundedElastic()),
            this::releaseSession,
            (session, error) -> releaseSession(session),
            this::releaseSession
        );
    }

    @Override
    public Mono<Void> ensureTable(String metric, List<SchemaColumn> columns) {
        Objects.requireNonNull(columns, "columns");
        String table = tableName(metric);
        List<ColumnSpec> requested = normalizeColumns(columns);
        String definitions = requested.stream()
            .map(column -> IoTDBSqlUtils.quoteIdentifier(column.name)
                + " " + column.type.name()
                + (column.category == ColumnCategory.TAG ? " TAG" : " FIELD"))
            .reduce((left, right) -> left + ", " + right)
            .orElse("");

        String createDatabase = "CREATE DATABASE IF NOT EXISTS " + IoTDBSqlUtils.quoteIdentifier(database);
        String createTable = "CREATE TABLE IF NOT EXISTS " + table + " (" + definitions + ")";

        return execute(createDatabase)
            .then(execute(createTable))
            .then(query("DESCRIBE " + table).collectList())
            .flatMap(rows -> reconcileSchema(table, requested, rows))
            .doOnSuccess(merged -> schemas.put(metric, List.copyOf(merged)))
            .then();
    }

    @Override
    public Mono<Void> write(String metric, Flux<TimeSeriesData> data, Set<String> tagColumns) {
        Objects.requireNonNull(data, "data");
        Set<String> tags = tagColumns == null ? Collections.emptySet() : new LinkedHashSet<>(tagColumns);
        return schemaFor(metric)
            .flatMapMany(schema -> {
                validateTags(schema, tags);
                return Flux.usingWhen(
                    acquireSession(),
                    session -> data
                        .buffer(writeBatchSize)
                        .concatMap(batch -> Mono.fromRunnable(() -> writeBatch(session, metric, schema, batch))
                            .subscribeOn(Schedulers.boundedElastic()), 1)
                        .thenMany(Flux.empty()),
                    this::releaseSession,
                    (session, error) -> releaseSession(session),
                    this::releaseSession
                );
            })
            .then();
    }

    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            pool.close();
            schemas.clear();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }

    private static ITableSessionPool createPool(IoTDBProperties properties) {
        if (properties.getNodeUrls() == null || properties.getNodeUrls().isEmpty()) {
            throw new IllegalArgumentException("At least one IoTDB node URL must be configured");
        }
        if (properties.getUsername() == null || properties.getUsername().trim().isEmpty()
            || properties.getPassword() == null || properties.getPassword().isEmpty()) {
            throw new IllegalArgumentException("IoTDB username and password must be configured");
        }
        if (properties.getMaxSize() < 1
            || properties.getConnectionTimeoutInMs() < 1
            || properties.getFetchSize() < 1
            || properties.getQueryTimeoutInMs() < 1
            || properties.getWriteBatchSize() < 1) {
            throw new IllegalArgumentException("IoTDB pool, timeout, fetch and batch sizes must be positive");
        }
        return new org.apache.iotdb.session.pool.TableSessionPoolBuilder()
            .nodeUrls(new ArrayList<>(properties.getNodeUrls()))
            .user(properties.getUsername())
            .password(properties.getPassword())
            .maxSize(properties.getMaxSize())
            .queryTimeoutInMs(properties.getQueryTimeoutInMs())
            .fetchSize(properties.getFetchSize())
            .connectionTimeoutInMs(properties.getConnectionTimeoutInMs())
            .build();
    }

    private Mono<ITableSession> acquireSession() {
        return Mono.fromCallable(() -> {
            if (disposed.get()) {
                throw new IllegalStateException("IoTDB operations have been disposed");
            }
            return pool.getSession();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> releaseSession(ITableSession session) {
        return Mono.<Void>fromRunnable(() -> closeSession(session)).subscribeOn(Schedulers.boundedElastic());
    }

    private static void closeSession(ITableSession session) {
        try {
            session.close();
        } catch (Throwable error) {
            throw propagate("Failed to release IoTDB session", error);
        }
    }

    private <T> T withSession(SessionCallback<T> callback) {
        if (disposed.get()) {
            throw new IllegalStateException("IoTDB operations have been disposed");
        }
        ITableSession session = null;
        try {
            session = pool.getSession();
            return callback.apply(session);
        } catch (Throwable error) {
            throw propagate("IoTDB operation failed", error);
        } finally {
            if (session != null) {
                closeSession(session);
            }
        }
    }

    private Flux<Map<String, Object>> rows(SessionDataSet dataSet) {
        List<String> columns = dataSet.getColumnNames();
        return Flux.generate(sink -> readNext(dataSet, columns, sink));
    }

    private static void readNext(SessionDataSet dataSet,
                                 List<String> columns,
                                 SynchronousSink<Map<String, Object>> sink) {
        try {
            if (!dataSet.hasNext()) {
                sink.complete();
                return;
            }
            sink.next(toMap(columns, dataSet.next()));
        } catch (Throwable error) {
            sink.error(propagate("Failed to read IoTDB query result", error));
        }
    }

    private static Map<String, Object> toMap(List<String> columns, RowRecord row) {
        List<Field> fields = row.getFields();
        Map<String, Object> result = new LinkedHashMap<>(columns.size());
        int columnOffset = columns.size() == fields.size() + 1 ? 1 : 0;
        if (columnOffset == 1 && isTimeColumn(columns.get(0))) {
            result.put(normalizeResultColumn(columns.get(0)), row.getTimestamp());
        } else if (columns.size() != fields.size()) {
            throw new IllegalStateException("IoTDB result column count does not match its row values");
        }

        for (int index = 0; index < fields.size(); index++) {
            String column = columns.get(index + columnOffset);
            result.put(normalizeResultColumn(column), fieldValue(fields.get(index)));
        }
        return result;
    }

    private static String normalizeResultColumn(String column) {
        if (isTimeColumn(column)) {
            return "time".equalsIgnoreCase(column) ? "time" : column;
        }
        return IoTDBSqlUtils.decodeColumnName(column);
    }

    private static boolean isTimeColumn(String column) {
        return "time".equalsIgnoreCase(column)
            || "timestamp".equalsIgnoreCase(column)
            || "_ts".equalsIgnoreCase(column);
    }

    private static Object fieldValue(Field field) {
        TSDataType type = field.getDataType();
        if (type == null) {
            return null;
        }
        if (type == TSDataType.STRING || type == TSDataType.TEXT) {
            return field.getStringValue();
        }
        if (type == TSDataType.BLOB && field.getBinaryV() != null) {
            return field.getBinaryV().getValues();
        }
        return field.getObjectValue(type);
    }

    private Mono<List<ColumnSpec>> schemaFor(String metric) {
        String table = tableName(metric);
        List<ColumnSpec> cached = schemas.get(metric);
        if (cached != null) {
            return Mono.just(cached);
        }
        return query("DESCRIBE " + table)
            .collectList()
            .map(rows -> parseSchema(rows, table))
            .doOnNext(schema -> schemas.putIfAbsent(metric, schema));
    }

    private Mono<List<ColumnSpec>> reconcileSchema(String table,
                                                   List<ColumnSpec> requested,
                                                   List<Map<String, Object>> describedRows) {
        Map<String, ColumnSpec> existing = new LinkedHashMap<>();
        for (ColumnSpec column : parseSchema(describedRows, table)) {
            existing.put(column.name, column);
        }

        List<ColumnSpec> toAdd = new ArrayList<>();
        for (ColumnSpec requestedColumn : requested) {
            ColumnSpec current = existing.get(requestedColumn.name);
            if (current == null) {
                toAdd.add(requestedColumn);
                continue;
            }
            if (current.type != requestedColumn.type || current.category != requestedColumn.category) {
                throw new IllegalStateException("IoTDB table schema mismatch for column "
                    + IoTDBSqlUtils.decodeColumnName(requestedColumn.name));
            }
        }

        return Flux.fromIterable(toAdd)
            .concatMap(column -> execute("ALTER TABLE " + table + " ADD COLUMN "
                + IoTDBSqlUtils.quoteIdentifier(column.name) + " " + column.type.name()
                + (column.category == ColumnCategory.TAG ? " TAG" : " FIELD"))
                .thenReturn(column))
            .collectList()
            .map(added -> {
                added.forEach(column -> existing.put(column.name, column));
                return List.copyOf(existing.values());
            });
    }

    private static List<ColumnSpec> parseSchema(List<Map<String, Object>> rows, String table) {
        Map<String, ColumnSpec> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String name = value(row, "ColumnName", "Column Name", "column_name");
            if (name == null || isTimeColumn(name)) {
                continue;
            }
            String dataType = value(row, "DataType", "Data Type", "data_type");
            String category = value(row, "Category", "category");
            if (dataType == null || category == null) {
                throw new IllegalStateException("Unexpected DESCRIBE output for IoTDB table " + table);
            }
            TSDataType type = parseType(dataType);
            ColumnCategory columnCategory = parseCategory(category);
            result.put(name, new ColumnSpec(name, type, columnCategory));
        }
        return List.copyOf(result.values());
    }

    private static String value(Map<String, Object> row, String... names) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            for (String name : names) {
                if (name.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
                    return String.valueOf(entry.getValue());
                }
            }
        }
        return null;
    }

    private static List<ColumnSpec> normalizeColumns(List<SchemaColumn> columns) {
        Map<String, ColumnSpec> normalized = new LinkedHashMap<>();
        for (SchemaColumn column : columns) {
            if (column == null) {
                throw new IllegalArgumentException("Schema columns must not contain null entries");
            }
            String physicalName = IoTDBSqlUtils.encodeColumnName(column.getName());
            if ("time".equals(physicalName)) {
                throw new IllegalArgumentException("The IoTDB time column is implicit and cannot be declared");
            }
            TSDataType type = parseType(column.getType());
            if (column.isTag() && type != TSDataType.STRING) {
                throw new IllegalArgumentException("IoTDB TAG columns must use STRING type");
            }
            ColumnSpec spec = new ColumnSpec(physicalName, type,
                column.isTag() ? ColumnCategory.TAG : ColumnCategory.FIELD);
            ColumnSpec previous = normalized.putIfAbsent(physicalName, spec);
            if (previous != null && (previous.type != spec.type || previous.category != spec.category)) {
                throw new IllegalArgumentException("Conflicting IoTDB schema for column " + column.getName());
            }
        }
        return List.copyOf(normalized.values());
    }

    private static TSDataType parseType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("IoTDB column type must not be empty");
        }
        TSDataType parsed;
        try {
            parsed = TSDataType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Unsupported IoTDB Table type: " + type, error);
        }
        if (!TABLE_TYPES.contains(parsed)) {
            throw new IllegalArgumentException("Unsupported Apache IoTDB Table type: " + type);
        }
        return parsed;
    }

    private static ColumnCategory parseCategory(String category) {
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if ("TAG".equals(normalized)) {
            return ColumnCategory.TAG;
        }
        if ("ATTRIBUTE".equals(normalized)) {
            return ColumnCategory.ATTRIBUTE;
        }
        if ("FIELD".equals(normalized)) {
            return ColumnCategory.FIELD;
        }
        throw new IllegalStateException("Unsupported IoTDB column category: " + category);
    }

    private static void validateTags(List<ColumnSpec> schema, Set<String> logicalTags) {
        Set<String> expected = new LinkedHashSet<>();
        for (String tag : logicalTags) {
            expected.add(IoTDBSqlUtils.encodeColumnName(tag));
        }
        Set<String> registered = new LinkedHashSet<>();
        for (ColumnSpec column : schema) {
            if (column.category == ColumnCategory.TAG) {
                registered.add(column.name);
            }
        }
        if (!expected.equals(registered)) {
            throw new IllegalStateException("IoTDB TAG columns do not match the registered table schema");
        }
    }

    private void writeBatch(ITableSession session,
                           String metric,
                           List<ColumnSpec> schema,
                           List<TimeSeriesData> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<String> names = new ArrayList<>(schema.size());
        List<TSDataType> types = new ArrayList<>(schema.size());
        List<ColumnCategory> categories = new ArrayList<>(schema.size());
        Map<String, ColumnSpec> byLogicalName = new HashMap<>(schema.size());
        for (ColumnSpec column : schema) {
            names.add(column.name);
            types.add(column.type);
            categories.add(column.category);
            byLogicalName.put(IoTDBSqlUtils.decodeColumnName(column.name), column);
        }

        Tablet tablet = new Tablet(IoTDBSqlUtils.encodeMetricName(metric), names, types, categories, rows.size());
        try {
            session.executeNonQueryStatement("USE " + IoTDBSqlUtils.quoteIdentifier(database));
        } catch (Throwable error) {
            throw propagate("Failed to select IoTDB database", error);
        }
        for (TimeSeriesData row : rows) {
            if (row == null) {
                throw new IllegalArgumentException("IoTDB write data must not contain null rows");
            }
            Map<String, Object> values = row.getData() == null ? Collections.emptyMap() : row.getData();
            validateDataColumns(values, byLogicalName);
            int rowIndex = tablet.getRowSize();
            tablet.addTimestamp(rowIndex, row.getTimestamp());
            for (ColumnSpec column : schema) {
                String logicalName = IoTDBSqlUtils.decodeColumnName(column.name);
                Object value = values.get(logicalName);
                tablet.addValue(column.name, rowIndex, toTabletValue(value, column));
            }
        }
        try {
            session.insert(tablet);
        } catch (Throwable error) {
            throw propagate("Failed to insert IoTDB tablet", error);
        }
    }

    private static void validateDataColumns(Map<String, Object> values, Map<String, ColumnSpec> schema) {
        for (String name : values.keySet()) {
            if (isTimeColumn(name)) {
                continue;
            }
            String physical = IoTDBSqlUtils.encodeColumnName(name);
            if (!schema.containsKey(IoTDBSqlUtils.decodeColumnName(physical))) {
                throw new IllegalArgumentException("No registered IoTDB schema column for " + name);
            }
        }
    }

    private static Object toTabletValue(Object value, ColumnSpec column) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> || value instanceof Collection<?> || value.getClass().isArray()) {
            if (column.type == TSDataType.STRING || column.type == TSDataType.TEXT) {
                return ObjectMappers.toJsonString(value);
            }
            throw new IllegalArgumentException("Nested values require a STRING or TEXT IoTDB column");
        }
        if (column.category == ColumnCategory.TAG) {
            return scalarString(value);
        }
        switch (column.type) {
            case BOOLEAN:
                return value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value));
            case INT32:
                return number(value).intValue();
            case INT64:
                return number(value).longValue();
            case TIMESTAMP:
                return timestamp(value);
            case FLOAT:
                return number(value).floatValue();
            case DOUBLE:
                return number(value).doubleValue();
            case DATE:
                if (value instanceof LocalDate) {
                    return value;
                }
                if (value instanceof Date) {
                    return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                return LocalDate.parse(String.valueOf(value));
            case STRING:
            case TEXT:
                return scalarString(value);
            case BLOB:
                if (value instanceof byte[]) {
                    return value;
                }
                return scalarString(value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            default:
                throw new IllegalArgumentException("Unsupported Apache IoTDB value type: " + column.type);
        }
    }

    private static String scalarString(Object value) {
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Date) {
            return Instant.ofEpochMilli(((Date) value).getTime()).toString();
        }
        return String.valueOf(value);
    }

    private static Number number(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("IoTDB numeric column received a non-numeric value", error);
        }
    }

    private static long timestamp(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof Instant) {
            return ((Instant) value).toEpochMilli();
        }
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toInstant().toEpochMilli();
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toInstant().toEpochMilli();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String tableName(String metric) {
        return IoTDBSqlUtils.quoteIdentifier(database) + "."
            + IoTDBSqlUtils.quoteIdentifier(IoTDBSqlUtils.encodeMetricName(metric));
    }

    private static void closeDataSet(SessionDataSet dataSet) {
        try {
            dataSet.close();
        } catch (Throwable error) {
            throw propagate("Failed to close IoTDB query result", error);
        }
    }

    private static RuntimeException propagate(String message, Throwable error) {
        if (error instanceof RuntimeException) {
            return (RuntimeException) error;
        }
        return new IllegalStateException(message, error);
    }

    @FunctionalInterface
    private interface SessionCallback<T> {
        T apply(ITableSession session) throws IoTDBConnectionException, StatementExecutionException;
    }

    private static final class ColumnSpec {
        private final String name;
        private final TSDataType type;
        private final ColumnCategory category;

        private ColumnSpec(String name, TSDataType type, ColumnCategory category) {
            this.name = name;
            this.type = type;
            this.category = category;
        }
    }
}
