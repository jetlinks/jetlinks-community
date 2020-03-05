package org.jetlinks.community.elastic.search.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketAggregationsStructure;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketResponse;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponse;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface AggregationService {

    Mono<MetricsResponse> metricsAggregation(String index, QueryParam queryParam, MetricsAggregationStructure structure);

    Mono<BucketResponse> bucketAggregation(String index, QueryParam queryParam, BucketAggregationsStructure structure);

    Flux<Map<String, Object>> aggregation(String index, AggregationQueryParam queryParam);

    default Flux<Map<String, Object>> aggregation(ElasticIndex index, AggregationQueryParam queryParam) {
        return aggregation(index.getIndex(), queryParam);
    }
}
