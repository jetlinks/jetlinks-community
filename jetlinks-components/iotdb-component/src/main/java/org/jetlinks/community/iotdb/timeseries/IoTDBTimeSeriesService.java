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

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.iotdb.IoTDBOperations;
import org.jetlinks.community.iotdb.utils.IoTDBQueryUtils;
import org.jetlinks.community.iotdb.utils.IoTDBSqlUtils;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationColumn;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.community.timeseries.query.Group;
import org.jetlinks.community.timeseries.query.TimeGroup;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class IoTDBTimeSeriesService implements TimeSeriesService {

    private final String metric;
    private final IoTDBOperations operations;

    public IoTDBTimeSeriesService(String metric, IoTDBOperations operations) {
        this.metric = metric;
        this.operations = operations;
    }

    @Override
    public Flux<TimeSeriesData> query(QueryParam queryParam) {
        QueryParamEntity param = toEntity(queryParam);
        return operations.query(IoTDBQueryUtils.buildSelectSql(metric, param, operations.getDatabase()))
            .map(IoTDBQueryUtils::convertToTimeSeriesData)
            .map(IoTDBTimeSeriesService::withTimestamp);
    }

    @Override
    public Flux<TimeSeriesData> multiQuery(Collection<QueryParam> queries) {
        if (queries == null || queries.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(queries).concatMap(this::query);
    }

    @Override
    public Mono<Integer> count(QueryParam queryParam) {
        QueryParamEntity param = toEntity(queryParam);
        return operations.query(IoTDBQueryUtils.buildCountSql(metric, param, operations.getDatabase()))
            .next()
            .map(row -> ((Number) row.getOrDefault("total", 0)).intValue())
            .defaultIfEmpty(0);
    }

    @Override
    public <T> Mono<PagerResult<T>> queryPager(QueryParam queryParam, Function<TimeSeriesData, T> mapper) {
        QueryParamEntity param = toEntity(queryParam);
        QueryParamEntity countParam = param.clone().noPaging();
        return Mono.zip(
            count(countParam),
            query(param).map(mapper).collectList(),
            (total, data) -> PagerResult.of(total, data, param));
    }

    @Override
    public Flux<AggregationData> aggregation(AggregationQueryParam param) {
        if (param == null || param.getAggColumns() == null || param.getAggColumns().isEmpty()) {
            return Flux.error(new IllegalArgumentException("At least one aggregation column is required"));
        }
        List<Group> groups = param.getGroups();
        TimeGroup timeGroup = groups.stream()
            .filter(TimeGroup.class::isInstance)
            .map(TimeGroup.class::cast)
            .findFirst().orElse(null);
        List<String> selections = new ArrayList<>();
        List<String> groupBy = new ArrayList<>();
        Map<String, String> outputAliases = new HashMap<>();
        String timeAlias = "iotdb_time_bucket";
        if (timeGroup != null) {
            String expression = IoTDBSqlUtils.timeBucket(timeGroup.getInterval());
            selections.add(expression + " AS " + IoTDBSqlUtils.quoteIdentifier(timeAlias));
            outputAliases.put(timeAlias, timeGroup.getAlias());
            groupBy.add(expression);
        }
        int groupIndex = 0;
        for (Group group : groups) {
            if (group instanceof TimeGroup) {
                continue;
            }
            String column = IoTDBSqlUtils.quoteIdentifier(IoTDBSqlUtils.encodeColumnName(group.getProperty()));
            String alias = "iotdb_group_" + groupIndex++;
            selections.add(column + " AS " + IoTDBSqlUtils.quoteIdentifier(alias));
            outputAliases.put(alias, group.getAlias());
            groupBy.add(column);
        }
        int aggregateIndex = 0;
        for (AggregationColumn column : param.getAggColumns()) {
            String source = IoTDBSqlUtils.quoteIdentifier(IoTDBSqlUtils.encodeColumnName(column.getProperty()));
            String expression = column.getAggregation() == Aggregation.DISTINCT_COUNT
                ? "COUNT(DISTINCT " + source + ")"
                : aggregationFunction(column.getAggregation()) + "(" + source + ")";
            String alias = "iotdb_agg_" + aggregateIndex++;
            selections.add(expression + " AS " + IoTDBSqlUtils.quoteIdentifier(alias));
            outputAliases.put(alias, column.getAlias());
        }

        QueryParamEntity filters = param.getQueryParam() == null
            ? QueryParamEntity.of()
            : param.getQueryParam().clone();
        filters.noPaging();
        org.hswebframework.ezorm.core.param.Term from = new org.hswebframework.ezorm.core.param.Term();
        from.setColumn("timestamp");
        from.setTermType("gte");
        from.setValue(param.getStartWithTime());
        org.hswebframework.ezorm.core.param.Term to = new org.hswebframework.ezorm.core.param.Term();
        to.setColumn("timestamp");
        to.setTermType("lte");
        to.setValue(param.getEndWithTime());
        List<org.hswebframework.ezorm.core.param.Term> filterTerms = filters.getTerms() == null
            ? new ArrayList<>()
            : new ArrayList<>(filters.getTerms());
        filterTerms.add(from);
        filterTerms.add(to);
        filters.setTerms(filterTerms);

        StringBuilder sql = new StringBuilder("SELECT ")
            .append(String.join(", ", selections))
            .append(" FROM ")
            .append(IoTDBSqlUtils.quoteIdentifier(operations.getDatabase()))
            .append('.')
            .append(IoTDBSqlUtils.quoteIdentifier(IoTDBSqlUtils.encodeMetricName(metric)));
        String where = IoTDBSqlUtils.buildWhere(filters.getTerms());
        if (!where.isEmpty()) {
            sql.append(' ').append(where);
        }
        if (!groupBy.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", groupBy));
        }
        if (param.getLimit() > 0) {
            sql.append(" LIMIT ").append(param.getLimit());
        }

        DateTimeFormatter formatter = timeGroup == null || timeGroup.getFormat() == null
            ? null
            : DateTimeFormatter.ofPattern(timeGroup.getFormat(), Locale.ROOT).withZone(ZoneId.systemDefault());
        return operations.query(sql.toString()).map(row -> {
            Map<String, Object> values = new HashMap<>();
            row.forEach((name, value) -> values.put(outputAliases.getOrDefault(name, name), value));
            if (timeGroup != null && formatter != null) {
                Object timestamp = values.get(timeGroup.getAlias());
                long epoch = timestamp instanceof Number
                    ? ((Number) timestamp).longValue()
                    : timestamp instanceof Date
                    ? ((Date) timestamp).getTime()
                    : Instant.parse(String.valueOf(timestamp)).toEpochMilli();
                values.put(timeGroup.getAlias(), formatter.format(Instant.ofEpochMilli(epoch)));
            }
            return AggregationData.of(values);
        });
    }

    @Override
    public Mono<Void> commit(Publisher<TimeSeriesData> data) {
        return save(data);
    }

    @Override
    public Mono<Void> commit(TimeSeriesData data) {
        return save(Flux.just(data));
    }

    @Override
    public Mono<Void> save(Publisher<TimeSeriesData> data) {
        return operations.write(metric, Flux.from(data).map(IoTDBTimeSeriesService::withIdentity),
                                java.util.Collections.singleton(ThingsDataConstants.COLUMN_ID));
    }

    private static TimeSeriesData withIdentity(TimeSeriesData source) {
        Map<String, Object> data = new HashMap<>(source.getData());
        data.put(ThingsDataConstants.COLUMN_TIMESTAMP, source.getTimestamp());
        data.computeIfAbsent(ThingsDataConstants.COLUMN_ID, ignore -> IDGenerator.RANDOM.generate());
        return TimeSeriesData.of(source.getTimestamp(), data);
    }

    private static TimeSeriesData withTimestamp(TimeSeriesData data) {
        data.getData().putIfAbsent(ThingsDataConstants.COLUMN_TIMESTAMP, data.getTimestamp());
        return data;
    }

    private static String aggregationFunction(Aggregation aggregation) {
        if (aggregation == null) {
            throw new IllegalArgumentException("Aggregation function is required");
        }
        switch (aggregation) {
            case COUNT:
                return "COUNT";
            case SUM:
                return "SUM";
            case AVG:
                return "AVG";
            case MIN:
                return "MIN";
            case MAX:
                return "MAX";
            default:
                throw new UnsupportedOperationException("IoTDB Table aggregation does not support " + aggregation);
        }
    }

    private static QueryParamEntity toEntity(QueryParam param) {
        if (param == null) {
            return QueryParamEntity.of();
        }
        if (param instanceof QueryParamEntity) {
            return (QueryParamEntity) param;
        }
        QueryParamEntity entity = QueryParamEntity.of();
        entity.setTerms(param.getTerms());
        entity.setSorts(param.getSorts());
        entity.setPaging(param.isPaging());
        entity.setPageIndex(param.getPageIndex());
        entity.setPageSize(param.getPageSize());
        return entity;
    }
}
