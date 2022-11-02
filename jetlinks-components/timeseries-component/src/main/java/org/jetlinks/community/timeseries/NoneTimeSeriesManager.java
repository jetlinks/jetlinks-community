package org.jetlinks.community.timeseries;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class NoneTimeSeriesManager implements TimeSeriesManager {
    public static final NoneTimeSeriesService NONE = new NoneTimeSeriesService();

    @Override
    public TimeSeriesService getService(TimeSeriesMetric metric) {
        return NONE;
    }

    @Override
    public TimeSeriesService getServices(TimeSeriesMetric... metric) {
        return NONE;
    }

    @Override
    public TimeSeriesService getServices(String... metric) {
        return NONE;
    }

    @Override
    public TimeSeriesService getService(String metric) {
        return NONE;
    }

    @Override
    public Mono<Void> registerMetadata(TimeSeriesMetadata metadata) {
        return Mono.empty();
    }

    static class NoneTimeSeriesService implements TimeSeriesService {
        @Override
        public Flux<TimeSeriesData> query(QueryParam queryParam) {
            return Flux.empty();
        }

        @Override
        public Flux<TimeSeriesData> multiQuery(Collection<QueryParam> query) {
            return Flux.empty();
        }

        @Override
        public Mono<Integer> count(QueryParam queryParam) {
            return Mono.empty();
        }

        @Override
        public Flux<AggregationData> aggregation(AggregationQueryParam queryParam) {
            return Flux.empty();
        }

        @Override
        public Mono<Void> commit(Publisher<TimeSeriesData> data) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> commit(TimeSeriesData data) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> save(Publisher<TimeSeriesData> data) {
            return Mono.empty();
        }
    }
}
