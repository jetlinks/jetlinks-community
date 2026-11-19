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
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.FloatType;
import org.jetlinks.core.metadata.types.GeoType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.Interval;
import org.jetlinks.community.iotdb.IoTDBOperations;
import org.jetlinks.community.iotdb.SchemaColumn;
import org.jetlinks.community.iotdb.utils.IoTDBQueryUtils;
import org.jetlinks.community.iotdb.utils.IoTDBSqlUtils;
import org.jetlinks.community.things.data.DefaultMetricMetadataManager;
import org.jetlinks.community.things.data.MetricMetadataManager;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.ThingsDataUtils;
import org.jetlinks.community.things.data.operations.AbstractDDLOperations;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

final class IoTDBThingDataHelper {

    private final IoTDBOperations operations;
    private final MetricMetadataManager metadataManager = new DefaultMetricMetadataManager();
    private final Map<String, Set<String>> registeredTags = new ConcurrentHashMap<>();

    IoTDBThingDataHelper(IoTDBOperations operations) {
        this.operations = operations;
    }

    Mono<Void> registerMetric(AbstractDDLOperations.MetricType metricType,
                              String metric,
                              List<PropertyMetadata> properties,
                              boolean rowMode,
                              String thingIdColumn) {
        Map<String, SchemaColumn> columns = new LinkedHashMap<>();
        addColumn(columns, ThingsDataConstants.COLUMN_ID, "STRING", true);
        addColumn(columns, thingIdColumn, "STRING", true);
        if (rowMode && metricType == AbstractDDLOperations.MetricType.properties) {
            addColumn(columns, ThingsDataConstants.COLUMN_PROPERTY_ID, "STRING", true);
        }
        for (PropertyMetadata property : properties) {
            String name = property.getId();
            if (ThingsDataConstants.COLUMN_TIMESTAMP.equals(name)) {
                continue;
            }
            boolean tag = rowMode
                && metricType == AbstractDDLOperations.MetricType.properties
                && ThingsDataConstants.COLUMN_PROPERTY_ID.equals(name);
            addColumn(columns, name, typeOf(property.getValueType()), tag);
        }
        return operations.ensureTable(metric, new ArrayList<>(columns.values()))
            .then(Mono.fromRunnable(() -> {
                metadataManager.register(metric, properties);
                Set<String> tags = new LinkedHashSet<>();
                columns.values().stream().filter(SchemaColumn::isTag).map(SchemaColumn::getName).forEach(tags::add);
                registeredTags.put(metric, Set.copyOf(tags));
            }));
    }

    private static void addColumn(Map<String, SchemaColumn> columns,
                                  String name,
                                  String type,
                                  boolean tag) {
        if (name == null || name.isEmpty() || "time".equalsIgnoreCase(name)) {
            return;
        }
        SchemaColumn previous = columns.get(name);
        if (previous == null || tag && !previous.isTag()) {
            columns.put(name, new SchemaColumn(name, type, tag));
        }
    }

    private static String typeOf(DataType type) {
        if (type == null) {
            return "STRING";
        }
        if (type instanceof BooleanType) {
            return "BOOLEAN";
        }
        if (type instanceof DateTimeType) {
            return "TIMESTAMP";
        }
        if (type instanceof LongType) {
            return "INT64";
        }
        if (type instanceof IntType) {
            return "INT32";
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

    Mono<Void> doSave(String metric, TimeSeriesData data) {
        return doSave(metric, Flux.just(data));
    }

    Mono<Void> doSave(String metric, Flux<TimeSeriesData> data) {
        Set<String> tagColumns = registeredTags.get(metric);
        if (tagColumns == null || tagColumns.isEmpty()) {
            return Mono.error(new IllegalStateException("IoTDB schema has not been registered for metric: " + metric));
        }
        return operations.write(metric, data.map(IoTDBThingDataHelper::withoutTimestamp), tagColumns);
    }

    private static TimeSeriesData withoutTimestamp(TimeSeriesData data) {
        Map<String, Object> values = new LinkedHashMap<>(data.getData());
        values.remove(ThingsDataConstants.COLUMN_TIMESTAMP);
        values.remove("time");
        return TimeSeriesData.of(data.getTimestamp(), values);
    }

    Flux<TimeSeriesData> doQuery(String metric, QueryParamEntity param) {
        QueryParamEntity prepared = prepareQuery(metric, param);
        String sql = IoTDBQueryUtils.buildSelectSql(metric, prepared, (List<Term>) null, operations.getDatabase());
        return operations.query(sql).map(IoTDBQueryUtils::convertToTimeSeriesData);
    }

    <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                         QueryParamEntity param,
                                         Function<TimeSeriesData, T> mapper) {
        QueryParamEntity prepared = prepareQuery(metric, param);
        String countSql = IoTDBQueryUtils.buildCountSql(metric, prepared, (List<Term>) null, operations.getDatabase());
        String selectSql = IoTDBQueryUtils.buildSelectSql(metric, prepared, (List<Term>) null, operations.getDatabase());
        Mono<Integer> count = operations.query(countSql)
            .singleOrEmpty()
            .map(row -> CastUtils.castNumber(row.getOrDefault("total", 0)).intValue())
            .defaultIfEmpty(0);
        Mono<List<T>> data = operations.query(selectSql)
            .map(IoTDBQueryUtils::convertToTimeSeriesData)
            .map(mapper)
            .collectList();
        return Mono.zip(count, data, (total, rows) -> PagerResult.of(total, rows, param));
    }

    private QueryParamEntity prepareQuery(String metric, QueryParamEntity source) {
        QueryParamEntity prepared = source == null ? QueryParamEntity.of() : source.clone();
        if (CollectionUtils.isEmpty(prepared.getTerms())) {
            return prepared;
        }
        List<Term> terms = new ArrayList<>(prepared.getTerms().size());
        for (Term term : prepared.getTerms()) {
            terms.add(prepareTerm(metric, term));
        }
        prepared.setTerms(terms);
        return prepared;
    }

    private Term prepareTerm(String metric, Term source) {
        Term term = source.clone();
        if (term.getTerms() != null && !term.getTerms().isEmpty()) {
            List<Term> children = new ArrayList<>(term.getTerms().size());
            for (Term child : term.getTerms()) {
                children.add(prepareTerm(metric, child));
            }
            term.setTerms(children);
        }
        if (term.getColumn() == null) {
            return term;
        }
        metadataManager.getColumn(metric, term.getColumn())
            .map(PropertyMetadata::getValueType)
            .filter(Converter.class::isInstance)
            .map(Converter.class::cast)
            .ifPresent(converter -> {
                String type = term.getTermType() == null ? "" : term.getTermType().toLowerCase();
                if (Set.of("in", "nin", "btw", "nbtw").contains(type)) {
                    term.setValue(convertCollection(term.getValue(), converter));
                } else if (term.getValue() != null) {
                    term.setValue(converter.convert(term.getValue()));
                }
            });
        return term;
    }

    private static Object convertCollection(Object value, Converter<?> converter) {
        if (value instanceof Collection<?>) {
            List<Object> converted = new ArrayList<>(((Collection<?>) value).size());
            for (Object item : (Collection<?>) value) {
                converted.add(item == null ? null : converter.convert(item));
            }
            return converted;
        }
        if (value instanceof Object[]) {
            List<Object> converted = new ArrayList<>(((Object[]) value).length);
            for (Object item : (Object[]) value) {
                converted.add(item == null ? null : converter.convert(item));
            }
            return converted;
        }
        return value == null ? null : converter.convert(value);
    }

    Flux<AggregationData> aggregate(String metric,
                                    QueryParamEntity filter,
                                    Date from,
                                    Date to,
                                    Interval interval,
                                    String format,
                                    int limit,
                                    PropertyAggregation[] properties,
                                    boolean rowMode) {
        if (properties == null || properties.length == 0) {
            return Flux.error(new IllegalArgumentException("At least one IoTDB aggregation property is required"));
        }
        if (interval == null) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (PropertyAggregation property : properties) {
                values.put(property.getAlias(), property.getDefaultValue());
            }
            return Flux.fromArray(properties)
                .concatMap(property -> aggregationQuery(metric, filter, from, to, null, property, rowMode)
                    .next()
                    .doOnNext(row -> values.put(property.getAlias(), row.get(property.getAlias())))
                    .then())
                .then(Mono.fromSupplier(() -> AggregationData.of(values)))
                .flux();
        }

        NavigableMap<Long, Map<String, Object>> prepared = ThingsDataUtils.prepareAggregationData(
            new org.jetlinks.community.things.data.AggregationRequest(interval, format, from, to, limit, filter), properties);
        return Flux.fromArray(properties)
            .concatMap(property -> aggregationQuery(metric, filter, from, to, interval, property, rowMode)
                .doOnNext(row -> {
                    long timestamp = timestamp(row);
                    long bucket = interval.round(timestamp);
                    Map<String, Object> target = ThingsDataUtils.findAggregationData(bucket, prepared);
                    if (target != null && row.containsKey(property.getAlias())) {
                        target.put(property.getAlias(), row.get(property.getAlias()));
                    }
                })
                .then())
            .thenMany(Flux.fromIterable(prepared.values()).map(AggregationData::of).take(limit));
    }

    private Flux<Map<String, Object>> aggregationQuery(String metric,
                                                       QueryParamEntity filter,
                                                       Date from,
                                                       Date to,
                                                       Interval interval,
                                                       PropertyAggregation property,
                                                       boolean rowMode) {
        String field = rowMode
            ? (property.getAgg() == Aggregation.COUNT ? ThingsDataConstants.COLUMN_PROPERTY_ID : ThingsDataConstants.COLUMN_PROPERTY_NUMBER_VALUE)
            : property.getProperty();
        String aggregate = aggregationFunction(property.getAgg(), field);
        List<Term> extraTerms = new ArrayList<>();
        extraTerms.add(Term.of(ThingsDataConstants.COLUMN_TIMESTAMP, TermType.btw, Arrays.asList(from, to)));
        if (rowMode) {
            extraTerms.add(Term.of(ThingsDataConstants.COLUMN_PROPERTY_ID, TermType.eq, property.getProperty()));
        }
        String where = combineWhere(filter == null ? null : prepareQuery(metric, filter).getTerms(), extraTerms);
        String table = IoTDBSqlUtils.quoteQualifiedIdentifier(
            operations.getDatabase() + "." + IoTDBSqlUtils.encodeMetricName(metric));
        String alias = IoTDBSqlUtils.quoteIdentifier(IoTDBSqlUtils.encodeColumnName(property.getAlias()));
        StringBuilder sql = new StringBuilder("SELECT ")
            .append(aggregate)
            .append(" AS ").append(alias)
            .append(" FROM ").append(table)
            .append(where);
        if (interval != null) {
            String timeGroup = IoTDBSqlUtils.timeBucket(interval);
            sql.insert("SELECT ".length(), timeGroup + " AS \"_ts\", ");
            sql.append(" GROUP BY ").append(timeGroup);
        }
        return operations.query(sql.toString());
    }

    private static String aggregationFunction(Aggregation aggregation, String field) {
        if (aggregation == Aggregation.NONE) {
            throw new UnsupportedOperationException("IoTDB does not support the NONE aggregation");
        }
        String column = IoTDBSqlUtils.quoteQualifiedIdentifier(IoTDBSqlUtils.encodeColumnName(field));
        switch (aggregation) {
            case COUNT:
                return "count(" + column + ")";
            case DISTINCT_COUNT:
                return "count(distinct " + column + ")";
            case AVG:
                return "avg(" + column + ")";
            case MAX:
                return "max(" + column + ")";
            case MIN:
                return "min(" + column + ")";
            case SUM:
                return "sum(" + column + ")";
            default:
                throw new UnsupportedOperationException("IoTDB does not support aggregation: " + aggregation);
        }
    }

    private static String combineWhere(List<Term> terms, List<Term> extraTerms) {
        String where = IoTDBSqlUtils.buildWhere(terms);
        String extra = IoTDBSqlUtils.buildWhere(extraTerms);
        if (where.isEmpty()) {
            return extra.isEmpty() ? "" : " " + extra;
        }
        if (extra.isEmpty()) {
            return " " + where;
        }
        return " " + where + " AND " + extra.substring("WHERE ".length());
    }

    private static long timestamp(Map<String, Object> row) {
        Object value = row.get("time");
        if (value == null) {
            value = row.get("_ts");
        }
        if (value == null) {
            value = row.get("timestamp");
        }
        if (value == null) {
            throw new IllegalArgumentException("IoTDB interval aggregation result did not include the time bucket");
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        return CastUtils.castDate(value).getTime();
    }
}
