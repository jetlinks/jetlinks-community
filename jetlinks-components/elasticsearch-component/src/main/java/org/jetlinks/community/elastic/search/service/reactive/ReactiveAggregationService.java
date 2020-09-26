package org.jetlinks.community.elastic.search.service.reactive;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.Version;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.DefaultElasticSearchService;
import org.jetlinks.community.elastic.search.utils.ElasticSearchConverter;
import org.jetlinks.community.timeseries.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class ReactiveAggregationService implements AggregationService {

    private final ReactiveElasticsearchClient restClient;

    private final ElasticSearchIndexManager indexManager;

    @Autowired
    public ReactiveAggregationService(ElasticSearchIndexManager indexManager,
                                      ReactiveElasticsearchClient restClient) {
        this.restClient = restClient;
        this.indexManager = indexManager;
    }

    private Mono<SearchSourceBuilder> createSearchSourceBuilder(QueryParam queryParam, String index) {

        return indexManager
            .getIndexMetadata(index)
            .map(metadata -> ElasticSearchConverter.convertSearchSourceBuilder(queryParam, metadata));
    }

    private AggregationBuilder createBuilder(Group group, AggregationQueryParam param) {

        if (group instanceof TimeGroup) {
            TimeGroup timeGroup = ((TimeGroup) group);
            DateHistogramAggregationBuilder builder = AggregationBuilders
                .dateHistogram(timeGroup.getAlias())
                .field(timeGroup.getProperty());
            if (StringUtils.hasText(timeGroup.getFormat())) {
                String format = timeGroup.getFormat();
                if (format.startsWith("yyyy")) {
                    format = "8" + format;
                }
                builder.format(format);
            }
            builder.order(BucketOrder.key(false));
            if (timeGroup.getInterval() != null) {
                if (restClient.serverVersion().after(Version.V_7_2_0)) {
                    if (timeGroup.getInterval().isFixed()) {
                        builder.fixedInterval(new DateHistogramInterval(timeGroup.getInterval().toString()));
                    } else {
                        builder.calendarInterval(new DateHistogramInterval(timeGroup.getInterval().toString()));
                    }
                } else {
                    builder.dateHistogramInterval(new DateHistogramInterval(timeGroup.getInterval().toString()));
                }
            }

            builder.extendedBounds(getExtendedBounds(param));
//            builder.missing("");

            builder.timeZone(ZoneId.systemDefault());
            return builder;
        } else {
            TermsAggregationBuilder builder = AggregationBuilders
                .terms(group.getAlias())
                .field(group.getProperty());
            if (group instanceof LimitGroup) {
                builder.size(((LimitGroup) group).getLimit());
            } else {
                builder.size(100);
            }
//            builder.missing(0);
            return builder.executionHint("map");
        }
    }

    @Override
    public Flux<Map<String, Object>> aggregation(String[] index, AggregationQueryParam aggregationQueryParam) {
        QueryParam queryParam = prepareQueryParam(aggregationQueryParam);

        List<Group> groups = new ArrayList<>();
        // TODO: 2020/9/3
        if (aggregationQueryParam.getGroupByTime() != null) {
            groups.add(aggregationQueryParam.getGroupByTime());
        }
        groups.addAll(aggregationQueryParam.getGroupBy());
        AggregationBuilder aggregationBuilder;
        AggregationBuilder lastAggBuilder;
        if (!groups.isEmpty()) {
            Group first = groups.get(0);
            aggregationBuilder = lastAggBuilder = createBuilder(first, aggregationQueryParam);
            for (int i = 1; i < groups.size(); i++) {
                aggregationBuilder.subAggregation(lastAggBuilder = createBuilder(groups.get(i), aggregationQueryParam));
            }
        } else {
            aggregationBuilder = lastAggBuilder = AggregationBuilders.count("count");
        }
        for (AggregationColumn aggColumn : aggregationQueryParam.getAggColumns()) {
            AggregationBuilder builder = AggType.of(aggColumn.getAggregation().name())
                .aggregationBuilder(aggColumn.getAlias(), aggColumn.getProperty());
            if (builder instanceof TopHitsAggregationBuilder) {
                TopHitsAggregationBuilder topHitsBuilder = ((TopHitsAggregationBuilder) builder);
                if (CollectionUtils.isEmpty(queryParam.getSorts())) {
                    topHitsBuilder.sort(aggregationQueryParam.getTimeProperty(), SortOrder.DESC);
                } else {
                    topHitsBuilder.sorts(queryParam.getSorts()
                        .stream()
                        .map(sort -> SortBuilders.fieldSort(sort.getName())
                            .order("desc".equalsIgnoreCase(sort.getOrder()) ? SortOrder.DESC : SortOrder.ASC))
                        .collect(Collectors.toList()));
                }
                if (aggColumn instanceof LimitAggregationColumn) {
                    topHitsBuilder.size(((LimitAggregationColumn) aggColumn).getLimit());
                }else {
                    topHitsBuilder.size(1);
                }
            }
            lastAggBuilder.subAggregation(builder);
        }

        AggregationBuilder ageBuilder = aggregationBuilder;

        return Flux.fromArray(index)
            .flatMap(idx -> Mono.zip(indexManager.getIndexStrategy(idx), Mono.just(idx)))
            .collectList()
            .flatMap(strategy ->
                this
                    .createSearchSourceBuilder(queryParam, index[0])
                    .map(builder ->
                        new SearchRequest(strategy
                            .stream()
                            .map(tp2 -> tp2.getT1().getIndexForSearch(tp2.getT2()))
                            .toArray(String[]::new))
                            .indicesOptions(DefaultElasticSearchService.indexOptions)
                            .source(builder.size(0).aggregation(ageBuilder))
                    )
            )
            .flatMap(restClient::searchForPage)
            .flatMapMany(this::parseResult)
            .as(flux -> aggregationQueryParam.getLimit() > 0 ? flux.take(aggregationQueryParam.getLimit()) : flux)
            ;
    }

    protected Flux<Map<String, Object>> parseResult(SearchResponse searchResponse) {
        return Mono.justOrEmpty(searchResponse.getAggregations())
            .flatMapIterable(Aggregations::asList)
            .flatMap(agg -> parseAggregation(agg.getName(), agg));
    }

    private Flux<Map<String, Object>> parseAggregation(String name, org.elasticsearch.search.aggregations.Aggregation aggregation) {
        if (aggregation instanceof Terms) {
            return parseAggregation(((Terms) aggregation));
        }
        if (aggregation instanceof TopHits) {
            TopHits topHits = ((TopHits) aggregation);
            return Flux
                .fromArray(topHits.getHits().getHits())
                .map(hit -> {
                    Map<String, Object> val = hit.getSourceAsMap();
                    if (!val.containsKey("id")) {
                        val.put("id", hit.getId());
                    }
                    return val;
                });
        }
        if (aggregation instanceof Histogram) {
            return parseAggregation(((Histogram) aggregation));
        }
        if (aggregation instanceof ValueCount) {
            return Flux.just(Collections.singletonMap(name, ((ValueCount) aggregation).getValue()));
        }
        if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
            return Flux.just(Collections.singletonMap(name, getSafeNumber(((NumericMetricsAggregation.SingleValue) aggregation).value())));
        }

        return Flux.empty();
    }

    private double getSafeNumber(double number) {
        return (Double.isNaN(number) || Double.isInfinite(number)) ? 0D : number;
    }

    private Flux<Map<String, Object>> parseAggregation(Histogram aggregation) {

        return Flux
            .fromIterable(aggregation.getBuckets())
            .flatMap(bucket ->
                Flux.fromIterable(bucket.getAggregations().asList())
                    .flatMap(agg -> this.parseAggregation(agg.getName(), agg))
                    .defaultIfEmpty(Collections.emptyMap())
                    .map(map -> {
                        Map<String, Object> val = new HashMap<>(map);
                        val.put(aggregation.getName(), bucket.getKeyAsString());
                        val.put("_" + aggregation.getName(), bucket.getKey());
                        return val;
                    })
            );
    }

    private Flux<Map<String, Object>> parseAggregation(Terms aggregation) {

        return Flux.fromIterable(aggregation.getBuckets())
            .flatMap(bucket -> Flux.fromIterable(bucket.getAggregations().asList())
                .flatMap(agg -> parseAggregation(agg.getName(), agg)
                    .map(map -> {
                        Map<String, Object> val = new HashMap<>(map);
                        val.put(aggregation.getName(), bucket.getKeyAsString());
                        return val;
                    })
                ));
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

    protected static ExtendedBounds getExtendedBounds(AggregationQueryParam param) {
        return new ExtendedBounds(calculateStartWithTime(param), param.getEndWithTime());
    }

    private static long calculateStartWithTime(AggregationQueryParam param) {
        long startWithParam = param.getStartWithTime();

//        if (param.getGroupByTime() != nullcalculateStartWithTime(param) && param.getGroupByTime().getInterval() != null) {
//            long timeInterval = param.getGroupByTime().getInterval().toMillis() * param.getLimit();
//            long tempStartWithParam = param.getEndWithTime() - timeInterval;
//            startWithParam = Math.max(tempStartWithParam, startWithParam);
//        }
        return startWithParam;
    }

}
