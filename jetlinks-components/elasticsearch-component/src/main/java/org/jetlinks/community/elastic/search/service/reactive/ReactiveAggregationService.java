package org.jetlinks.community.elastic.search.service.reactive;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.Version;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.LongBounds;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.*;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.Interval;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.utils.ElasticSearchConverter;
import org.jetlinks.community.timeseries.query.*;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.5
 **/
@Slf4j
public class ReactiveAggregationService implements AggregationService {

    private final ReactiveElasticsearchClient restClient;

    private final ElasticSearchIndexManager indexManager;

    //是否打开执行提示，打开后，聚合查询时会给每一个bucket构造global ordinals。单个索引数据量大于一百万时建议关闭。
    private static final boolean IS_OPEN_GLOBAL_ORDINALS =
        Boolean.parseBoolean(System.getProperty("elasticsearch.agg.query.execution_hint", "false"));

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
            builder.timeZone(ZoneId.systemDefault());
            builder.order(BucketOrder.key(false));
            if (timeGroup.getInterval() != null) {
                Interval interval = timeGroup.getInterval();
                String intervalString = interval.toString();
                if (restClient.serverVersion().after(Version.V_7_2_0)) {
                    if (DateHistogramAggregationBuilder.DATE_FIELD_UNITS.containsKey(intervalString)) {
                        builder.calendarInterval(new DateHistogramInterval(intervalString));
                    } else {
                        builder.fixedInterval(new DateHistogramInterval(intervalString));
//                        builder.dateHistogramInterval(new DateHistogramInterval(intervalString));
                    }
                } else {
                    builder.dateHistogramInterval(new DateHistogramInterval(intervalString));
                }
            }

            builder.extendedBounds(getExtendedBounds(param));
//            builder.missing("");

            return builder;
        } else {
            TermsAggregationBuilder builder = AggregationBuilders
                .terms(group.getAlias())
                .field(group.getProperty());
            if (group instanceof LimitGroup) {
                if (((LimitGroup) group).getLimit() > 0) {
                    builder.size(((LimitGroup) group).getLimit());
                }
            } else {
                builder.size(100);
            }
            //直接进行子聚合的计算
            builder.collectMode(Aggregator.SubAggCollectionMode.DEPTH_FIRST);
//            builder.missing(0);
            if (IS_OPEN_GLOBAL_ORDINALS) {
                builder.executionHint("map");
            }
            return builder;
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
        List<AggregationBuilder> aggs = new ArrayList<>();

        AggregationBuilder aggregationBuilder = null;
        AggregationBuilder lastAgg = null;
        if (!groups.isEmpty()) {
            Group first = groups.get(0);
            aggregationBuilder = lastAgg = createBuilder(first, aggregationQueryParam);
            for (int i = 1; i < groups.size(); i++) {
                aggregationBuilder.subAggregation(lastAgg = createBuilder(groups.get(i), aggregationQueryParam));
            }
            aggs.add(aggregationBuilder);
        }

        boolean group = aggregationBuilder != null;
        for (AggregationColumn aggColumn : aggregationQueryParam.getAggColumns()) {
            AggregationBuilder builder = AggType
                .of(aggColumn.getAggregation().name())
                .aggregationBuilder(aggColumn.getAlias(), aggColumn.getProperty());

            if (builder instanceof ValuesSourceAggregationBuilder &&
                aggColumn.getDefaultValue() != null) {
                ((ValuesSourceAggregationBuilder<?>) builder)
                    .missing(aggColumn.getDefaultValue());
            }

            if (builder instanceof TopHitsAggregationBuilder) {
                TopHitsAggregationBuilder topHitsBuilder = ((TopHitsAggregationBuilder) builder);
                if (CollectionUtils.isEmpty(queryParam.getSorts())) {
                    topHitsBuilder.sort(aggregationQueryParam.getTimeProperty(), SortOrder.DESC);
                } else {
                    topHitsBuilder.sorts(queryParam
                                             .getSorts()
                                             .stream()
                                             .map(sort -> SortBuilders
                                                 .fieldSort(sort.getName())
                                                 .order("desc".equalsIgnoreCase(sort.getOrder())
                                                            ? SortOrder.DESC
                                                            : SortOrder.ASC))
                                             .collect(Collectors.toList()));
                }
                if (aggColumn instanceof LimitAggregationColumn) {
                    topHitsBuilder.size(((LimitAggregationColumn) aggColumn).getLimit());
                } else {
                    topHitsBuilder.size(1);
                }
            }
            if (group) {
                lastAgg.subAggregation(builder);
            } else {
                aggs.add(builder);
            }
        }

        return Flux
            .fromArray(index)
            .flatMap(idx -> Mono.zip(indexManager.getIndexStrategy(idx), Mono.just(idx)))
            .collectList()
            .flatMap(strategy -> this
                .createSearchSourceBuilder(queryParam, index[0])
                .map(builder -> {
                         aggs.forEach(builder.size(0)::aggregation);
                         return new SearchRequest(strategy
                                                      .stream()
                                                      .map(tp2 -> tp2
                                                          .getT1()
                                                          .getIndexForSearch(tp2.getT2()))
                                                      .toArray(String[]::new))
                             .indicesOptions(ReactiveElasticSearchService.indexOptions)
                             .source(builder);
                     }
                )
            )
            .flatMap(restClient::searchForPage)
            .flatMapMany(this::parseResult)
            .as(flux -> {
                if (!group) {
                    return flux
                        .map(Map::entrySet)
                        .flatMap(Flux::fromIterable)
                        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                        .flux();
                }
                return flux;
            })
            ;
    }

    protected Flux<Map<String, Object>> parseResult(SearchResponse searchResponse) {
        return Mono.justOrEmpty(searchResponse.getAggregations())
                   .flatMapIterable(Aggregations::asList)
                   .flatMap(agg -> parseAggregation(agg.getName(), agg), Integer.MAX_VALUE);
    }

    private Flux<Map<String, Object>> parseAggregation(String name,
                                                       org.elasticsearch.search.aggregations.Aggregation aggregation) {
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
            return Flux.just(Collections.singletonMap(name, getSafeNumber(((NumericMetricsAggregation.SingleValue) aggregation)
                                                                              .value())));
        }
        if (aggregation instanceof ExtendedStats) {
            ExtendedStats stats = ((ExtendedStats) aggregation);
            // TODO: 2020/10/29 只处理了标准差差
            return Flux.just(Collections.singletonMap(name, stats.getStdDeviation()));
        }

        return Flux.empty();
    }

    private Object getSafeNumber(double number) {
        return (Double.isNaN(number) || Double.isInfinite(number)) ? null : number;
    }

    private Flux<Map<String, Object>> parseAggregation(Histogram aggregation) {

        return Flux
            .fromIterable(aggregation.getBuckets())
            .flatMap(bucket ->
                         Flux.fromIterable(bucket.getAggregations().asList())
                             .flatMap(agg -> this.parseAggregation(agg.getName(), agg), Integer.MAX_VALUE)
                             .defaultIfEmpty(Collections.emptyMap())
//                    .map(Map::entrySet)
//                    .flatMap(Flux::fromIterable)
//                    .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                             .map(map -> {
                                 Map<String, Object> val = new HashMap<>(map);
                                 val.put(aggregation.getName(), bucket.getKeyAsString());
                                 val.put("_" + aggregation.getName(), bucket.getKey());
                                 return val;
                             }),
                     Integer.MAX_VALUE
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
        boolean hasTimestamp = false;
        for (Term term : queryParam.getTerms()) {
            if (param.getTimeProperty().equals(term.getColumn())) {
                hasTimestamp = true;
            }
        }
        if (!hasTimestamp) {
            queryParam.and(param.getTimeProperty(), TermType.btw, Arrays.asList(calculateStartWithTime(param), param.getEndWithTime()));
        }
        if (queryParam.getSorts().isEmpty()) {
            queryParam.orderBy(param.getTimeProperty()).desc();
        }
        return queryParam;
    }

    protected static LongBounds getExtendedBounds(AggregationQueryParam param) {

        return new LongBounds(calculateStartWithTime(param), param.getEndWithTime());
    }

    //聚合查询默认的时间间隔
    static long thirtyDayMillis = Duration
        .ofDays(Integer.getInteger("elasticsearch.agg.default-range-day", 90))
        .toMillis();

    static long calculateStartWithTime(AggregationQueryParam param) {
        long startWithParam = param.getStartWithTime();
        if (startWithParam == 0) {
            //从查询条件中提取时间参数来获取时间区间
            List<Term> terms = param.getQueryParam().getTerms();
            for (Term term : terms) {
                if ("timestamp".equals(term.getColumn())) {
                    Object value = term.getValue();
                    String termType = term.getTermType();
                    if (TermType.btw.equals(termType)) {
                        if (String.valueOf(value).contains(",")) {
                            value = Arrays.asList(String.valueOf(value).split(","));
                        }
                        return DateTimeType.GLOBAL.convert(CastUtils.castArray(value).get(0)).getTime();
                    }
                    if (TermType.gt.equals(termType) || TermType.gte.equals(termType)) {

                        return DateTimeType.GLOBAL.convert(value).getTime();
                    }
                }
            }
            return param.getEndWithTime() - thirtyDayMillis;
        }
        return startWithParam;
    }

}
