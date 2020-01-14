package org.jetlinks.community.elastic.search.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketAggregationsStructure;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketResponse;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponse;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface AggregationService {

    Mono<MetricsResponse> metricsAggregation(QueryParam queryParam, MetricsAggregationStructure structure, ElasticIndex provider);

    Mono<BucketResponse> bucketAggregation(QueryParam queryParam, BucketAggregationsStructure structure, ElasticIndex provider);
}
