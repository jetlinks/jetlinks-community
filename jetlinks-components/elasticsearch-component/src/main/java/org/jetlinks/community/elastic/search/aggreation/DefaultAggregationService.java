package org.jetlinks.community.elastic.search.aggreation;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketAggregationsStructure;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketResponse;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponse;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.elastic.search.parser.QueryParamTranslateService;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class DefaultAggregationService implements AggregationService {

    private final QueryParamTranslateService translateService;

    private final ElasticRestClient restClient;

    private final IndexOperationService indexOperationService;

    @Autowired
    public DefaultAggregationService(IndexOperationService indexOperationService,
                                     ElasticRestClient restClient,
                                     QueryParamTranslateService translateService) {
        this.indexOperationService = indexOperationService;
        this.restClient = restClient;
        this.translateService = translateService;
    }


    @Override
    public Mono<MetricsResponse> metricsAggregation(QueryParam queryParam,
                                                    MetricsAggregationStructure structure,
                                                    ElasticIndex provider) {
        return searchSourceBuilderMono(queryParam, provider)
            .doOnNext(builder -> builder.aggregation(
                structure.getType().aggregationBuilder(structure.getName(), structure.getField())))
            .map(builder -> new SearchRequest(provider.getStandardIndex())
                .source(builder))
            .flatMap(request -> Mono.<SearchResponse>create(monoSink ->
                restClient.getQueryClient().searchAsync(request, RequestOptions.DEFAULT, translatorActionListener(monoSink))))
            .map(searchResponse -> structure.getType().getResponse(structure.getName(), searchResponse));
    }

    @Override
    public Mono<BucketResponse> bucketAggregation(QueryParam queryParam, BucketAggregationsStructure structure, ElasticIndex provider) {
        return searchSourceBuilderMono(queryParam, provider)
            .doOnNext(builder ->
                builder.aggregation(structure.getType().aggregationBuilder(structure))
            )
            .map(builder -> new SearchRequest(provider.getStandardIndex())
                .source(builder))
            .doOnNext(searchRequest ->
                log.debug("聚合查询index:{},参数:{}",
                    provider.getStandardIndex(),
                    JSON.toJSON(searchRequest.source().toString())))
            .flatMap(request -> Mono.<SearchResponse>create(monoSink ->
                restClient.getQueryClient().searchAsync(request, RequestOptions.DEFAULT, translatorActionListener(monoSink))))
            .map(response -> structure.getType().convert(response.getAggregations().get(structure.getName())))
            .map(buckets -> BucketResponse.builder()
                .name(structure.getName())
                .buckets(buckets)
                .build()
            )
            ;

    }

    private Mono<SearchSourceBuilder> searchSourceBuilderMono(QueryParam queryParam, ElasticIndex provider) {
        QueryParam tempQueryParam = queryParam.clone();
        tempQueryParam.setPaging(false);
        return indexOperationService.getIndexMappingMetadata(provider.getStandardIndex())
            .map(metadata -> translateService.translate(tempQueryParam, metadata))
            .doOnError(e -> log.error("解析queryParam错误, index:{}", provider.getStandardIndex(), e));
        // return Mono.just(translateService.translate(queryParam, IndexMappingMetadata.getInstance(provider.getStandardIndex())));
    }

    private <T> ActionListener<T> translatorActionListener(MonoSink<T> sink) {
        return new ActionListener<T>() {
            @Override
            public void onResponse(T response) {
                sink.success(response);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ElasticsearchException) {
                    if (((ElasticsearchException) e).status().getStatus() == 404) {
                        sink.success();
                        return;
                    } else if (((ElasticsearchException) e).status().getStatus() == 400) {
                        sink.error(new ElasticsearchParseException("查询参数格式错误", e));
                    }
                }
                sink.error(e);
            }
        };
    }
}
