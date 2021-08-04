package org.jetlinks.community.elastic.search.aggreation;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.aggregations.bucket.histogram.LongBounds;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.aggreation.bucket.Bucket;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketAggregationsStructure;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketResponse;
import org.jetlinks.community.elastic.search.aggreation.bucket.Sort;
import org.jetlinks.community.elastic.search.aggreation.enums.BucketType;
import org.jetlinks.community.elastic.search.aggreation.enums.MetricsType;
import org.jetlinks.community.elastic.search.aggreation.enums.OrderType;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.DefaultElasticSearchService;
import org.jetlinks.community.elastic.search.utils.ElasticSearchConverter;
import org.jetlinks.community.elastic.search.utils.ReactorActionListener;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
//@Service
@Slf4j
public class DefaultAggregationService implements AggregationService {

    private final ElasticRestClient restClient;

    private final ElasticSearchIndexManager indexManager;

    @Autowired
    public DefaultAggregationService(ElasticSearchIndexManager indexManager,
                                     ElasticRestClient restClient) {
        this.restClient = restClient;
        this.indexManager = indexManager;
    }

//    @Override
//    public Mono<MetricsResponse> metricsAggregation(String index, QueryParam queryParam,
//                                                    MetricsAggregationStructure structure) {
//        return createSearchSourceBuilder(queryParam, index)
//            .map(builder -> new SearchRequest(index)
//                .source(builder.aggregation(structure.getType().aggregationBuilder(structure.getName(), structure.getField()))))
//            .flatMap(request -> Mono.<SearchResponse>create(monoSink ->
//                restClient.getQueryClient().searchAsync(request, RequestOptions.DEFAULT, translatorActionListener(monoSink))))
//            .map(searchResponse -> structure.getType().getResponse(structure.getName(), searchResponse));
//    }
//
//    @Override
//    public Mono<BucketResponse> bucketAggregation(String index, QueryParam queryParam, BucketAggregationsStructure structure) {
//        return createSearchSourceBuilder(queryParam, index)
//            .map(builder -> new SearchRequest(index)
//                .source(builder
//                    .aggregation(structure.getType().aggregationBuilder(structure))
//                    //.aggregation(AggregationBuilders.topHits("last_val").from(1))
//                ))
////            .doOnNext(searchRequest -> {
////                if (log.isDebugEnabled()) {
////                    log.debug("聚合查询ElasticSearch:{},参数:{}", index, JSON.toJSON(searchRequest.source().toString()));
////                }
////            })
//            .flatMap(request -> Mono.<SearchResponse>create(monoSink ->
//                restClient
//                    .getQueryClient()
//                    .searchAsync(request, RequestOptions.DEFAULT, translatorActionListener(monoSink))))
//            .map(response -> BucketResponse.builder()
//                .name(structure.getName())
//                .buckets(structure.getType().convert(response.getAggregations().get(structure.getName())))
//                .build())
//            ;
//
//    }

    private Mono<SearchSourceBuilder> createSearchSourceBuilder(QueryParam queryParam, String index) {

        return indexManager
            .getIndexMetadata(index)
            .map(metadata -> ElasticSearchConverter.convertSearchSourceBuilder(queryParam, metadata));
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

    @Override
    public Flux<Map<String, Object>> aggregation(String[] index, AggregationQueryParam aggregationQueryParam) {
        QueryParam queryParam = prepareQueryParam(aggregationQueryParam);
        BucketAggregationsStructure structure = createAggParameter(aggregationQueryParam);
        return Flux.fromArray(index)
            .flatMap(idx -> Mono.zip(indexManager.getIndexStrategy(idx), Mono.just(idx)))
            .collectList()
            .flatMap(strategy ->
                createSearchSourceBuilder(queryParam, index[0])
                    .map(builder ->
                        new SearchRequest(strategy
                            .stream()
                            .map(tp2 -> tp2.getT1().getIndexForSearch(tp2.getT2()))
                            .toArray(String[]::new))
                            .indicesOptions(DefaultElasticSearchService.indexOptions)
                            .source(builder.size(0).aggregation(structure.getType().aggregationBuilder(structure))
                            )
                    )
            )
            .flatMap(searchRequest ->
                ReactorActionListener
                    .<SearchResponse>mono(listener ->
                        restClient.getQueryClient()
                            .searchAsync(searchRequest, RequestOptions.DEFAULT, listener)
                    ))
            .filter(response -> response.getAggregations() != null)
            .map(response -> BucketResponse.builder()
                .name(structure.getName())
                .buckets(structure.getType().convert(response.getAggregations().get(structure.getName())))
                .build())
            .flatMapIterable(BucketsParser::convert)
            .take(aggregationQueryParam.getLimit())
            ;
    }

    static class BucketsParser {

        private final List<Map<String, Object>> result = new ArrayList<>();

        public static List<Map<String, Object>> convert(BucketResponse response) {
            return new BucketsParser(response).result;
        }

        public BucketsParser(BucketResponse response) {
            this(response.getBuckets());
        }

        public BucketsParser(List<Bucket> buckets) {
            buckets.forEach(bucket -> parser(bucket, new HashMap<>()));
        }

        public void parser(Bucket bucket, Map<String, Object> fMap) {
            addBucketProperty(bucket, fMap);
            if (bucket.getBuckets() != null && !bucket.getBuckets().isEmpty()) {
                bucket.getBuckets().forEach(b -> {
                    Map<String, Object> map = new HashMap<>(fMap);
                    addBucketProperty(b, map);
                    parser(b, map);
                });
            } else {
                result.add(fMap);
            }
        }

        private void addBucketProperty(Bucket bucket, Map<String, Object> fMap) {
            fMap.put(bucket.getName(), bucket.getKey());
            fMap.putAll(bucket.toMap());
        }
    }

    protected static QueryParam prepareQueryParam(AggregationQueryParam param) {
        QueryParam queryParam = param.getQueryParam().clone();
        queryParam.setPaging(false);
        queryParam.and(param.getTimeProperty(), TermType.btw, Arrays.asList(calculateStartWithTime(param), param.getEndWithTime()));
        if (queryParam.getSorts().isEmpty()) {
            queryParam.orderBy(param.getTimeProperty()).desc();
        }
        return queryParam;
    }

    protected BucketAggregationsStructure createAggParameter(AggregationQueryParam param) {
        List<BucketAggregationsStructure> structures = new ArrayList<>();
        if (param.getGroupByTime() != null) {
            structures.add(convertAggGroupTimeStructure(param));
        }
        if (param.getGroupBy() != null && !param.getGroupBy().isEmpty()) {
            structures.addAll(getTermTypeStructures(param));
        }
        for (int i = 0, size = structures.size(); i < size; i++) {
            if (i < size - 1) {
                structures.get(i).setSubBucketAggregation(Collections.singletonList(structures.get(i + 1)));
            }
            if (i == size - 1) {
                structures.get(i)
                    .setSubMetricsAggregation(param
                        .getAggColumns()
                        .stream()
                        .map(agg -> {
                            MetricsAggregationStructure metricsAggregationStructure = new MetricsAggregationStructure();
                            metricsAggregationStructure.setField(agg.getProperty());
                            metricsAggregationStructure.setName(agg.getAlias());
                            metricsAggregationStructure.setType(MetricsType.of(agg.getAggregation().name()));
                            return metricsAggregationStructure;
                        }).collect(Collectors.toList()));
            }
        }
        return structures.get(0);
    }

    protected BucketAggregationsStructure convertAggGroupTimeStructure(AggregationQueryParam param) {
        BucketAggregationsStructure structure = new BucketAggregationsStructure();
        structure.setInterval(param.getGroupByTime().getInterval().toString());
        structure.setType(BucketType.DATE_HISTOGRAM);
        structure.setFormat(param.getGroupByTime().getFormat());
        structure.setName(param.getGroupByTime().getAlias());
        structure.setField(param.getGroupByTime().getProperty());
        structure.setSort(Sort.desc(OrderType.KEY));
        structure.setExtendedBounds(getExtendedBounds(param));
        return structure;
    }

    protected static LongBounds getExtendedBounds(AggregationQueryParam param) {
        return new LongBounds(calculateStartWithTime(param), param.getEndWithTime());
    }

    private static long calculateStartWithTime(AggregationQueryParam param) {
        long startWithParam = param.getStartWithTime();
//        if (param.getGroupByTime() != null && param.getGroupByTime().getInterval() != null) {
//            long timeInterval = param.getGroupByTime().getInterval().toMillis() * param.getLimit();
//            long tempStartWithParam = param.getEndWithTime() - timeInterval;
//            startWithParam = Math.max(tempStartWithParam, startWithParam);
//        }
        return startWithParam;
    }

    protected List<BucketAggregationsStructure> getTermTypeStructures(AggregationQueryParam param) {
        return param.getGroupBy()
            .stream()
            .map(group -> {
                BucketAggregationsStructure structure = new BucketAggregationsStructure();
                structure.setType(BucketType.TERMS);
                structure.setSize(param.getLimit());
                structure.setField(group.getProperty());
                structure.setName(group.getAlias());
                return structure;
            }).collect(Collectors.toList());
    }
}
