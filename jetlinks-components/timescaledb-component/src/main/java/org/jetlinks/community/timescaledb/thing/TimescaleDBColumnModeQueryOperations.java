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
package org.jetlinks.community.timescaledb.thing;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.ezorm.rdb.mapping.defaults.record.Record;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.dml.QueryOperator;
import org.hswebframework.ezorm.rdb.operator.dml.query.NativeSelectColumn;
import org.hswebframework.ezorm.rdb.operator.dml.query.SelectColumn;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.ThingsDataUtils;
import org.jetlinks.community.things.data.operations.ColumnModeQueryOperationsBase;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeQueryOperationsBase;
import org.jetlinks.community.timescaledb.TimescaleDBUtils;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

import static org.jetlinks.community.timescaledb.TimescaleDBUtils.createTimeGroupColumn;

@Slf4j
public class TimescaleDBColumnModeQueryOperations extends ColumnModeQueryOperationsBase {
    private final DatabaseOperator database;

    public TimescaleDBColumnModeQueryOperations(String thingType,
                                                String thingTemplateId,
                                                String thingId,
                                                MetricBuilder metricBuilder,
                                                DataSettings settings,
                                                ThingsRegistry registry,
                                                DatabaseOperator database) {
        super(thingType, thingTemplateId, thingId, metricBuilder, settings, registry);
        this.database = database;
    }

    @Override
    protected Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query) {
        metric = TimescaleDBUtils.getTableName(metric);
        return query
            .execute(
                database
                    .dml()
                    .createReactiveRepository(metric)
                    .createQuery()::setParam
            )
            .fetch()
            .map(this::convertToTimeSeriesData)
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }

    @Override
    protected <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                                   Query<?, QueryParamEntity> query,
                                                   Function<TimeSeriesData, T> mapper) {
        return QueryHelper
            .queryPager(
                query.getParam(),
                () -> database
                    .dml()
                    .createReactiveRepository(TimescaleDBUtils.getTableName(metric))
                    .createQuery(),
                record -> mapper.apply(convertToTimeSeriesData(record))
            )
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }

    static final String timestampAlias = "_ts";

    @Override
    protected Flux<AggregationData> doAggregation(String metric,
                                                  AggregationRequest request,
                                                  AggregationContext context) {
        return doAggregation0(database, metric, request, context);
    }


    static Flux<AggregationData> doAggregation0(DatabaseOperator database,
                                                String metric,
                                                AggregationRequest request,
                                                AggregationContext context) {
        metric = TimescaleDBUtils.getTableName(metric);
        QueryParamEntity filter = request.getFilter();
        filter.setSorts(new ArrayList<>());
        filter.setPaging(false);
        QueryOperator query = database.dml().query(metric);

        //按时间分组
        if (request.getInterval() != null) {
            NativeSelectColumn column = createTimeGroupColumn(
                request.getFrom().getTime(),
                request.getInterval()
            );
            query.groupBy(column);
            query.select(column);
            column.setAlias(timestampAlias);
        }

        for (PropertyAggregation property : context.getProperties()) {
            SelectColumn column = new SelectColumn();
            column.setColumn(property.getProperty());
            column.setAlias(property.getAlias());
            TimescaleDBUtils.applyAggColumn(property.getAgg(), column);

            query.select(column);
        }

        query.where(cdt -> {
            cdt.each(filter.getTerms(), Conditional::accept);
            cdt.between(ThingsDataConstants.COLUMN_TIMESTAMP, request.getFrom(), request.getTo());
        });

        NavigableMap<Long, Map<String, Object>>
            prepares = ThingsDataUtils.prepareAggregationData(request, context.getProperties());

        return query
            .fetch(ResultWrappers.map())
            .reactive()
            .map(val -> Maps.filterValues(val, Objects::nonNull))
            .map(AggregationData::of)
            .groupBy(data -> data.getLong(timestampAlias).orElse(0L), Integer.MAX_VALUE)
            .flatMap(group -> {
                long time = group.key();
                Map<String, Object> prepare = ThingsDataUtils.findAggregationData(time, prepares);
                if (prepare == null) {
                    return Mono.empty();
                }
                return group
                    .doOnNext(data -> {
                        for (PropertyAggregation property : context.getProperties()) {
                            String alias = property.getAlias();
                            data.get(alias)
                                .ifPresent(val -> prepare.put(alias, val));
                        }
                    });
            })
            .thenMany(Flux.fromIterable(prepares.values()))
            .map(AggregationData::of)
            .take((long) request.getLimit() * context.getProperties().length)
            .contextWrite(ctx -> ctx.put(Logger.class, log));

    }


    private TimeSeriesData convertToTimeSeriesData(Record record) {
        return TimeSeriesData.of(
            record.get(ThingsDataConstants.COLUMN_TIMESTAMP)
                  .map(val -> CastUtils.castNumber(val).longValue())
                  .orElseGet(System::currentTimeMillis),
            record
        );
    }


    protected String createAggFunction(Aggregation aggregation) {
        switch (aggregation) {
            case COUNT:
                return "count(1)";
            case DISTINCT_COUNT:
                return "count(distinct \"numberValue\")";
            case AVG:
                return "avg(\"numberValue\")";
            case MAX:
                return "max(\"numberValue\")";
            case MIN:
                return "min(\"numberValue\")";
            case SUM:
                return "sum(\"numberValue\")";
//            case STDDEV:
//                return "stddev(\"numberValue\")";
        }
        throw new UnsupportedOperationException("不支持的聚合函数:" + aggregation);
    }
}
