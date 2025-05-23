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
package org.jetlinks.community.timescaledb.timeseries;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.ezorm.rdb.operator.dml.QueryOperator;
import org.hswebframework.ezorm.rdb.operator.dml.query.NativeSelectColumn;
import org.hswebframework.ezorm.rdb.operator.dml.query.SelectColumn;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.timescaledb.TimescaleDBOperations;
import org.jetlinks.community.timescaledb.TimescaleDBUtils;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.*;
import org.jetlinks.community.timeseries.utils.TimeSeriesUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
public class TimescaleDBTimeSeriesService implements TimeSeriesService {
    private final String metric;
    private final TimescaleDBOperations operations;

    @Override
    public Flux<TimeSeriesData> query(QueryParam queryParam) {
        if (isTableNotExist()) {
            return Flux.empty();
        }
        return operations
            .database()
            .dml()
            .createReactiveRepository(metric)
            .createQuery()
            .setParam(queryParam)
            .fetch()
            .map(TimescaleDBUtils::convertToTimeSeriesData)
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }

    @Override
    public Flux<TimeSeriesData> multiQuery(Collection<QueryParam> query) {
        return Flux.fromIterable(query)
                   .flatMap(this::query, 8);
    }

    @Override
    public Mono<Integer> count(QueryParam queryParam) {
        if (isTableNotExist()) {
            return Reactors.ALWAYS_ZERO;
        }
        return operations
            .database()
            .dml()
            .createReactiveRepository(metric)
            .createQuery()
            .setParam(queryParam)
            .count()
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }

    @Override
    public Flux<AggregationData> aggregation(AggregationQueryParam param) {
        List<Group> groups = param.getGroups();
        TimeSeriesUtils.prepareAggregationQueryParam(param);

        long startWith = param.getStartWithTime();
        if (isTableNotExist()) {
            return Flux.empty();
        }

        QueryOperator query = operations
            .database()
            .dml()
            .query(metric)
            .setParam(param.getQueryParam().noPaging())
            .where(conditional -> conditional.between(ThingsDataConstants.COLUMN_TIMESTAMP, startWith, param.getEndWithTime()));

        for (AggregationColumn aggColumn : param.getAggColumns()) {
            SelectColumn column = SelectColumn
                .of(aggColumn.getProperty(),
                    aggColumn.getAlias());
            TimescaleDBUtils.applyAggColumn(aggColumn.getAggregation(), column);

            query.select(column);
        }
        TimeGroup _timeGroup = null;
        for (Group group : groups) {
            if (group instanceof TimeGroup) {
                _timeGroup = ((TimeGroup) group);
                NativeSelectColumn column = TimescaleDBUtils.createTimeGroupColumn(startWith, _timeGroup.getInterval());
                column.setColumn(ThingsDataConstants.COLUMN_TIMESTAMP);
                column.setAlias(group.getAlias());
                query.select(column);
                query.groupBy(column);
            } else {
                SelectColumn column = SelectColumn.of(group.getProperty(), group.getAlias());
                query.select(column);
                query.groupBy(column);
            }
        }
        TimeGroup timeGroup = _timeGroup;
        Map<Set<Object>, NavigableMap<Long, Map<String, Object>>>
            groupedResults = new ConcurrentHashMap<>();
        Set<Object> key = new LinkedHashSet<>();
        //无数据时返回默认统计，供前端显示坐标轴
        groupedResults
            .computeIfAbsent(key, (ignore) -> TimeSeriesUtils.prepareAggregationData(param, timeGroup));

        return query
            .fetch(ResultWrappers.map())
            .reactive()
            .as(flux -> {
                if (groups.isEmpty() || timeGroup == null) {
                    return flux;
                }

                return flux
                    .doOnNext(data -> {
                        for (Group group : groups) {
                            if (group instanceof TimeGroup) {
                                continue;
                            }
                            Object g = data.get(group.getAlias());
                            key.add(g);
                        }
                        NavigableMap<Long, Map<String, Object>> prepares = groupedResults
                            .computeIfAbsent(key, (ignore) -> TimeSeriesUtils.prepareAggregationData(param, timeGroup));
                        long ts = CastUtils.castNumber(data.getOrDefault(timeGroup.getAlias(), 0)).longValue();
                        Map<String, Object> prepare = TimeSeriesUtils.findAggregationData(ts, prepares);
                        //时间错误?
                        if (prepare == null) {
                            return;
                        }
                        FastBeanCopier.copy(data, prepare, timeGroup.getAlias());
                    })
                    .thenMany(Flux.defer(() -> Flux.fromIterable(groupedResults.values())))
                    .flatMapIterable(Map::values);
            })
            .map(AggregationData::of)
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }

    private boolean isTableNotExist() {
        return !operations.database()
                          .getMetadata()
                          .getTableOrView(metric, false)
                          .isPresent();
    }

    @Override
    public Mono<Void> commit(Publisher<TimeSeriesData> data) {
        return Flux
            .from(data)
            .flatMap(this::commit)
            .then();
    }

    @Override
    public Mono<Void> commit(TimeSeriesData data) {

        return operations.writer().save(metric, toTimeSeriesData(data));
    }

    @Override
    public Mono<Void> save(Publisher<TimeSeriesData> data) {

        return operations.writer().save(metric, Flux
            .from(data)
            .map(TimescaleDBTimeSeriesService::toTimeSeriesData))
            ;
    }

    private static Map<String, Object> toTimeSeriesData(TimeSeriesData data) {
        Map<String, Object> map = data.getData();
        map.put(ThingsDataConstants.COLUMN_TIMESTAMP, data.getTimestamp());
        map.computeIfAbsent(ThingsDataConstants.COLUMN_ID, ignore -> IDGenerator.RANDOM.generate());

        return data.getData();
    }
}
