package org.jetlinks.community.elastic.search.timeseries;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

@AllArgsConstructor
@Slf4j
public class ElasticSearchTimeSeriesService implements TimeSeriesService {

    private String index;

    private ElasticSearchService elasticSearchService;

    private AggregationService aggregationService;

    @Override
    public Flux<TimeSeriesData> query(QueryParam queryParam) {
        return elasticSearchService.query(index, queryParam, map -> TimeSeriesData.of((Long) map.get("timestamp"), map));
    }

    @Override
    public Mono<Integer> count(QueryParam queryParam) {
        return elasticSearchService
            .count(index, queryParam)
            .map(Long::intValue);
    }

    @Override
    public Flux<AggregationData> aggregation(AggregationQueryParam queryParam) {
        return aggregationService
            .aggregation(index, queryParam)
            .onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .map(AggregationData::of);

    }

    @Override
    public Mono<PagerResult<TimeSeriesData>> queryPager(QueryParam queryParam) {
        return elasticSearchService.queryPager(index, queryParam, map -> TimeSeriesData.of((Long) map.get("timestamp"), map));
    }

    @Override
    public <T> Mono<PagerResult<T>> queryPager(QueryParam queryParam, Function<TimeSeriesData, T> mapper) {
        return elasticSearchService.queryPager(index, queryParam, map -> mapper.apply(TimeSeriesData.of((Long) map.get("timestamp"), map)));
    }


    @Override
    public Mono<Void> save(Publisher<TimeSeriesData> data) {
        return Flux.from(data)
            .flatMap(this::save)
            .then();
    }

    @Override
    public Mono<Void> save(TimeSeriesData data) {
        return Mono.defer(() -> {
            Map<String, Object> mapData = data.getData();
            mapData.put("timestamp", data.getTimestamp());
            return elasticSearchService.commit(index, mapData);
        });
    }
}
