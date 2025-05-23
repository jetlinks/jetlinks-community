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
package org.jetlinks.community.elastic.search.timeseries;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class ElasticSearchTimeSeriesService implements TimeSeriesService {

    private final String[] index;

    private final ElasticSearchService elasticSearchService;

    private final AggregationService aggregationService;

    @Override
    public Flux<TimeSeriesData> query(QueryParam queryParam) {
        return elasticSearchService
            .query(index, applySort(queryParam),
                   this::convertTimeSeriesData);
    }

    @Override
    public Flux<TimeSeriesData> multiQuery(Collection<QueryParam> query) {
        return elasticSearchService.multiQuery(
            index,
            query.stream().peek(this::applySort).collect(Collectors.toList()),
            this::convertTimeSeriesData);
    }

    @Override
    public Mono<Integer> count(QueryParam queryParam) {
        return elasticSearchService
            .count(index, queryParam)
            .map(Long::intValue);
    }

    @Override
    public Mono<PagerResult<TimeSeriesData>> queryPager(QueryParam queryParam) {
        return elasticSearchService
            .queryPager(index, applySort(queryParam)
                , this::convertTimeSeriesData);
    }

    @Override
    public <T> Mono<PagerResult<T>> queryPager(QueryParam queryParam, Function<TimeSeriesData, T> mapper) {
        return elasticSearchService.queryPager(
            index,
            applySort(queryParam),
            map -> mapper.apply(this.convertTimeSeriesData(map)));
    }

    @Override
    public Flux<AggregationData> aggregation(AggregationQueryParam queryParam) {
        return aggregationService
            .aggregation(index, queryParam)
            .onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .map(AggregationData::of)
//            .as(flux -> queryParam.getLimit() > 0 ? flux.take(queryParam.getLimit()) : flux)
            ;

    }

    protected QueryParam applySort(QueryParam param) {
        if (CollectionUtils.isEmpty(param.getSorts())) {
            param.orderBy("timestamp").desc();
        }
        return param;
    }


    @Override
    public Mono<Void> commit(Publisher<TimeSeriesData> data) {
        return Flux.from(data)
                   .flatMap(this::commit)
                   .then();
    }

    @Override
    public Mono<Void> commit(TimeSeriesData data) {
        Map<String, Object> mapData = new HashMap<>(data.getData());
        mapData.put("timestamp", data.getTimestamp());
        mapData.computeIfAbsent("id", ignore -> IDGenerator.RANDOM.generate());
        return elasticSearchService.commit(index[0], mapData);
    }

    @Override
    public Mono<Void> save(Publisher<TimeSeriesData> dateList) {

        return elasticSearchService
            .save(index[0],
                  Flux.from(dateList)
                      .map(data -> {
                          Map<String, Object> mapData = new HashMap<>(data.getData());
                          mapData.put("timestamp", data.getTimestamp());
                          mapData.computeIfAbsent("id", ignore -> IDGenerator.RANDOM.generate());
                          return mapData;
                      }));
    }

    public TimeSeriesData convertTimeSeriesData(Map<String, Object> data) {
        return TimeSeriesData
            .of(CastUtils
                    .castNumber(data.getOrDefault("timestamp", 0))
                    .longValue(), data);
    }
}
