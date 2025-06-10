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
package org.jetlinks.community.elastic.search.service.reactive;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.Interval;
import org.jetlinks.community.elastic.search.ElasticSearchSupport;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.utils.QueryParamTranslator;
import org.jetlinks.community.timeseries.query.*;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.ZoneId;
import java.util.*;

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

    private Aggregation.Builder.ContainerBuilder createGroupAggregation(Group group,
                                                                        AggregationQueryParam param) {
        if (group instanceof TimeGroup timeGroup) {
            Interval interval = timeGroup.getInterval();
            return new Aggregation.Builder()
                .dateHistogram(his -> {
                    his.timeZone(ZoneId.systemDefault().toString());
                    his.field(timeGroup.getProperty());
                    his.order(Collections.singletonList(
                        NamedValue.of("_key", SortOrder.Desc)
                    ));
                    if (timeGroup.getOffset() > 0) {
                        his.offset(Time.of(b -> b.offset((int) timeGroup.getOffset())));
                    }
                    if (StringUtils.hasText(timeGroup.getFormat())) {
                        String format = timeGroup.getFormat();
                        if (format.startsWith("yyyy")) {
                            format = "8" + format;
                        }
                        his.format(format);
                    }
                    if (interval.getNumber().intValue() == 1) {
                        his.calendarInterval(convertCalendarInterval(interval.getExpression()));
                    } else {
                        his.fixedInterval(t -> t.time(interval.toString()));
                    }
                    his.extendedBounds(bounds -> bounds
                        .min(FieldDateMath.of(b -> b.value((double) calculateStartWithTime(param))))
                        .max(FieldDateMath.of(b -> b.value((double) param.getEndWithTime()))));
                    return his;
                });
        } else {
            return new Aggregation.Builder()
                .terms(terms -> {
                    terms.field(group.getProperty());
                    if (group instanceof LimitGroup) {
                        terms.size(((LimitGroup) group).getLimit());
                    } else {
                        terms.size(100);
                    }
                    if (IS_OPEN_GLOBAL_ORDINALS) {
                        terms.executionHint(TermsAggregationExecutionHint.Map);
                    }
                    return terms;
                });
        }
    }

    private CalendarInterval convertCalendarInterval(String expression) {
        return switch (expression) {
            case Interval.year -> CalendarInterval.Year;
            case Interval.quarter -> CalendarInterval.Quarter;
            case Interval.month -> CalendarInterval.Month;
            case Interval.weeks -> CalendarInterval.Week;
            case Interval.days -> CalendarInterval.Day;
            case Interval.hours -> CalendarInterval.Hour;
            case Interval.minutes -> CalendarInterval.Minute;
            case Interval.seconds -> CalendarInterval.Second;
            default -> throw new UnsupportedOperationException("不支持的时间间隔:" + expression);
        };
    }

    private Map<String, Aggregation> createAggregations(ElasticSearchIndexMetadata metadata,
                                                        AggregationQueryParam param) {

        Map<String, Aggregation> colAgg = Maps.newHashMapWithExpectedSize(param.getAggColumns().size());
        for (AggregationColumn aggColumn : param.getAggColumns()) {
            if (aggColumn instanceof LimitAggregationColumn limitAggregationColumn) {
                colAgg.put(
                    aggColumn.getAlias(),
                    Aggregation.of(builder -> builder
                        .topHits(top -> {
                            top
                                .size(limitAggregationColumn.getLimit())
                                .missing(aggColumn.getDefaultValue() == null
                                             ? null
                                             : FieldValue.of(JsonData.of(aggColumn.getDefaultValue())));
                            param.getQueryParam()
                                 .getSorts()
                                 .forEach(sort -> {
                                     if (StringUtils.hasText(sort.getName())) {
                                         top.sort(s -> s
                                             .field(f -> f
                                                 .field(sort.getName())
                                                 .order("desc".equalsIgnoreCase(sort.getOrder()) ? SortOrder.Desc : SortOrder.Asc)));
                                     }
                                 });
                            return top;
                        }))
                );
                continue;
            }
            colAgg.put(
                aggColumn.getAlias(),
                AggType
                    .of(aggColumn.getAggregation().name())
                    .aggregationBuilder(aggColumn.getAlias(),
                                        aggColumn.getProperty(),
                                        metadata,
                                        new Aggregation.Builder(),
                                        aggColumn.getDefaultValue())
                    .build()
            );
        }

        //不分组
        List<Group> groups = param.getGroups();
        if (CollectionUtils.isEmpty(groups)) {
            return colAgg;
        }
        Tuple2<String, Aggregation> frist = null;
        for (int i = groups.size() - 1; i >= 0; i--) {
            Group group = groups.get(i);
            Aggregation agg;
            if (frist == null) {
                //最后一个分组添加列聚合
                agg = createGroupAggregation(group, param).aggregations(colAgg).build();
            } else {
                agg = createGroupAggregation(group, param)
                    .aggregations(Collections.singletonMap(frist.getT1(), frist.getT2()))
                    .build();
                //添加上一个分组
            }
            frist = Tuples.of(group.getAlias(), agg);
        }

        if (frist == null) {
            return colAgg;
        }

        return Collections.singletonMap(frist.getT1(), frist.getT2());

    }

    @Override
    public Flux<Map<String, Object>> aggregation(String index, AggregationQueryParam aggregationQueryParam) {
        boolean isGroup = CollectionUtils.isNotEmpty(aggregationQueryParam.getGroups());

        QueryParamEntity param = aggregationQueryParam.getQueryParam().clone();
        param
            .toQuery()
            .between(aggregationQueryParam.getTimeProperty(),
                     aggregationQueryParam.getStartWithTime(),
                     aggregationQueryParam.getEndWithTime());


        return Mono.zip(
                       Mono.just(index),
                       indexManager
                           .getIndexStrategy(index)
                           .map(s -> s.getIndexForSearch(index)),
                       indexManager.getIndexMetadata(index))
                   .flatMapMany(tps -> {
                       ElasticSearchIndexMetadata metadata = tps.getT3();
                       return restClient
                           .execute(client -> client
                               .search(search -> search
                                   .index(tps.getT2())
                                   .query(q -> QueryParamTranslator.applyQueryBuilder(q, param, metadata))
                                   .ignoreUnavailable(true)
                                   .allowNoIndices(true)
                                   .size(0)
                                   .aggregations(createAggregations(metadata, aggregationQueryParam)), Map.class))
                           .flatMapMany(resp -> Flux
                               .fromIterable(resp.aggregations().entrySet())
                               .concatMap(e -> parseAggregation(e.getKey(), e.getValue(), -1)))
                           .as(flux -> {
                               if (!isGroup) {
                                   return flux
                                       .map(Map::entrySet)
                                       .flatMap(Flux::fromIterable)
                                       .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                                       .flux();
                               }
                               return flux;
                           });
                   });
    }

    @Override
    public Flux<Map<String, Object>> aggregation(String[] index, AggregationQueryParam aggregationQueryParam) {
        if (index.length == 1) {
            return aggregation(index[0], aggregationQueryParam);
        }
        boolean isGroup = CollectionUtils.isNotEmpty(aggregationQueryParam.getGroups());
        return Flux.fromArray(index)
                   .flatMap(idx ->
                                Mono.zip(Mono.just(idx),
                                         indexManager
                                             .getIndexStrategy(idx)
                                             .map(s -> s.getIndexForSearch(idx)),
                                         indexManager.getIndexMetadata(idx)))
                   .collectList()
                   .flatMapMany(tps -> {
                       ElasticSearchIndexMetadata metadata = tps.get(0).getT3();
                       return restClient
                           .execute(client -> client
                               .search(search -> search
                                   .index(Lists.transform(tps, Tuple3::getT2))
                                   .query(q -> QueryParamTranslator.applyQueryBuilder(q, aggregationQueryParam.getQueryParam(), metadata))
                                   .size(0)
                                   .ignoreUnavailable(true)
                                   .allowNoIndices(true)
                                   .aggregations(createAggregations(metadata, aggregationQueryParam)), Map.class))
                           .flatMapMany(resp -> Flux
                               .fromIterable(resp.aggregations().entrySet())
                               .concatMap(e -> parseAggregation(e.getKey(), e.getValue(), -1)))
                           .as(flux -> {
                               if (!isGroup) {
                                   return flux
                                       .map(Map::entrySet)
                                       .flatMap(Flux::fromIterable)
                                       .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                                       .flux();
                               }
                               return flux;
                           });
                   });

    }

    private Flux<Map<String, Object>> parseAggregation(String name,
                                                       Aggregate aggregate,
                                                       long docCount) {
        if (aggregate.isSum()) {
            return Flux.just(docCount == 0
                                 ? Collections.emptyMap()
                                 : Collections.singletonMap(name, getSafeNumber(aggregate.sum().value())));
        }
        if (aggregate.isAvg()) {
            return Flux.just(docCount == 0
                                 ? Collections.emptyMap()
                                 : Collections.singletonMap(name, getSafeNumber(aggregate.avg().value())));
        }
        if (aggregate.isMax()) {
            return Flux.just(docCount == 0
                                 ? Collections.emptyMap()
                                 : Collections.singletonMap(name, getSafeNumber(aggregate.max().value())));
        }
        if (aggregate.isMin()) {
            return Flux.just(docCount == 0
                                 ? Collections.emptyMap()
                                 : Collections.singletonMap(name, getSafeNumber(aggregate.min().value())));
        }
        if (aggregate.isCardinality()) {
            return Flux.just(Collections.singletonMap(name, aggregate.cardinality().value()));
        }
        if (aggregate.isFilter()) {
            return Flux.just(Collections.singletonMap(name, aggregate.filter().docCount()));
        }
        if (aggregate.isValueCount()) {
            return Flux.just(Collections.singletonMap(name, getSafeNumber(aggregate.valueCount().value())));
        }
        if (aggregate.isSterms()) {
            return parseAggregation(name, aggregate.sterms());
        }
        if (aggregate.isLterms()) {
            return parseAggregation(name, aggregate.lterms());
        }
        if (aggregate.isDterms()) {
            return parseAggregation(name, aggregate.dterms());
        }
        if (aggregate.isSimpleValue()) {
            return Flux.just(Collections.singletonMap(name, getSafeNumber(aggregate.simpleValue().value())));
        }

        if (aggregate.isExtendedStats()) {
            //只处理了标准差
            return Flux.just(Collections.singletonMap(name, getSafeNumber(aggregate.extendedStats().stdDeviation())));
        }

        if (aggregate.isHistogram()) {
            return parseAggregation(name, aggregate.histogram());
        }

        if (aggregate.isTopHits()) {
            return Flux
                .fromIterable(aggregate.topHits().hits().hits())
                .mapNotNull(hit -> {
                    JsonData source = hit.source();
                    if (source == null) {
                        return null;
                    }
                    @SuppressWarnings("all")
                    Map<String, Object> val = source.to(Map.class);
                    if (!val.containsKey("id")) {
                        val.put("id", hit.id());
                    }
                    return val;
                });
        }
        if (aggregate.isDateHistogram()) {
            return parseAggregation(name, aggregate.dateHistogram());
        }

        log.warn("unsupported aggregation {} : {}", aggregate._kind(), aggregate);
        return Flux.empty();
    }

    private Object getSafeNumber(double number) {
        return (Double.isNaN(number) || Double.isInfinite(number)) ? null : number;
    }

    private Flux<Map<String, Object>> parseAggregation(String name, MultiBucketAggregateBase<? extends MultiBucketBase> aggregation) {
        Buckets<? extends MultiBucketBase> buckets = aggregation.buckets();
        Flux<? extends MultiBucketBase> bucketFlux;
        if (buckets.isArray()) {
            bucketFlux = Flux.fromIterable(buckets.array());
        } else {
            bucketFlux = Flux.fromIterable(buckets.keyed().values());
        }
        return bucketFlux
            .concatMap(bucket -> Flux
                .fromIterable(bucket.aggregations().entrySet())
                .concatMap(e -> {

                    return parseAggregation(e.getKey(), e.getValue(), bucket.docCount());
                }, 0)
                .map(map -> transformBucket(name, map, bucket)), 0);

    }

    private Map<String, Object> transformBucket(String name,
                                                Map<String, Object> map,
                                                MultiBucketBase bucket) {
        return ElasticSearchSupport
            .current()
            .transformBucket(name, map, bucket);
    }


    private Object parseBucket(Object bucket) {
        if (bucket instanceof MultiBucketBase base) {
            return ElasticSearchSupport
                .current()
                .getBucketKey(base);
        }

        return null;
    }

    private Flux<Map<String, Object>> parseAggregation(String name, TermsAggregateBase<? extends MultiBucketBase> aggregation) {
        Buckets<? extends MultiBucketBase> buckets = aggregation.buckets();
        Flux<? extends MultiBucketBase> flux;
        if (buckets.isKeyed()) {
            flux = Flux.fromIterable(buckets.keyed().values());
        } else {
            flux = Flux.fromIterable(buckets.array());
        }

        return flux.concatMap(base -> Flux
            .fromIterable(base.aggregations().entrySet())
            .concatMap(e -> parseAggregation(e.getKey(), e.getValue(), base.docCount()), 0)
            .map(map -> {
                Map<String, Object> val = new HashMap<>(map);
                val.putIfAbsent(name, parseBucket(base));
                return val;
            }), 0);
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