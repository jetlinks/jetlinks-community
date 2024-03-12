package org.jetlinks.community.elastic.search.things;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.things.data.ThingPropertyDetail;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeQueryOperationsBase;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.*;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;

class ElasticSearchRowModeQueryOperations extends RowModeQueryOperationsBase {

    private final ElasticSearchService searchService;
    private final AggregationService aggregationService;

    public ElasticSearchRowModeQueryOperations(String thingType,
                                               String thingTemplateId,
                                               String thingId,
                                               MetricBuilder metricBuilder,
                                               DataSettings settings,
                                               ThingsRegistry registry,
                                               ElasticSearchService service,
                                               AggregationService aggregationService) {
        super(thingType, thingTemplateId, thingId, metricBuilder, settings, registry);
        this.searchService = service;
        this.aggregationService = aggregationService;
    }

    @Override
    protected Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query) {

        return searchService
            .query(metric,
                   query.getParam(),
                   data -> {
                       long ts = CastUtils.castNumber(data.getOrDefault("timestamp", 0L)).longValue();
                       return TimeSeriesData.of(ts, data);
                   });
    }

    @Override
    protected <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                                   Query<?, QueryParamEntity> query,
                                                   Function<TimeSeriesData, T> mapper) {
        return searchService
            .queryPager(metric,
                        query.getParam(),
                        data -> {
                            long ts = CastUtils.castNumber(data.getOrDefault("timestamp", 0L)).longValue();
                            return mapper.apply(TimeSeriesData.of(ts, data));
                        });
    }

    @Override
    protected Flux<ThingPropertyDetail> queryEachProperty(@Nonnull String metric,
                                                          @Nonnull Query<?, QueryParamEntity> query,
                                                          @Nonnull ThingMetadata metadata,
                                                          @Nonnull Map<String, PropertyMetadata> properties) {
        QueryParamEntity param = query.getParam();
        //不分页或者每页数量大于1000则回退到普通方式查询
        if (!param.isPaging() || param.getPageSize() >= 1000) {
            return super.queryEachProperty(metric, query, metadata, properties);
        }

        if (properties.size() <= 200) {
            query.in(ThingsDataConstants.COLUMN_PROPERTY_ID, properties.keySet());
        }

        //通过聚合查询求top n来查询每一个属性数据
        return AggregationQueryParam
            .of()
            .agg(new LimitAggregationColumn(ThingsDataConstants.COLUMN_PROPERTY_ID,
                                            ThingsDataConstants.COLUMN_PROPERTY_ID,
                                            Aggregation.FIRST,
                                            param.getPageSize()))
            .groupBy(new LimitGroup(ThingsDataConstants.COLUMN_PROPERTY_ID,
                                    ThingsDataConstants.COLUMN_PROPERTY_ID,
                                    param.getPageSize() * properties.size())) //按property分组
            .filter(param)
            .execute(params -> aggregationService.aggregation(metric, params))
            .mapNotNull(data -> {
                long ts = CastUtils.castNumber(data.getOrDefault(ThingsDataConstants.COLUMN_TIMESTAMP, 0L)).longValue();

                String property = (String) data.getOrDefault(ThingsDataConstants.COLUMN_PROPERTY_ID, null);
                String thingId = (String) data.getOrDefault(metricBuilder.getThingIdProperty(), null);
                Object value = data.getOrDefault(ThingsDataConstants.COLUMN_PROPERTY_VALUE, null);
                if (property == null || thingId == null || value == null) {
                    return null;
                }
                return applyProperty(
                    ThingPropertyDetail.of(TimeSeriesData.of(ts, data), properties.get(property)),
                    data);
            });
    }

    @Override
    protected Flux<AggregationData> doAggregation(String metric, AggregationRequest request, AggregationContext context) {
        PropertyAggregation[] properties = context.getProperties();
        //只聚合一个属性时
        if (properties.length == 1) {
            return AggregationQueryParam
                .of()
                .agg(ThingsDataConstants.COLUMN_PROPERTY_NUMBER_VALUE, properties[0].getAlias(), properties[0].getAgg(), properties[0].getDefaultValue())
                .as(param -> {
                    if (request.getInterval() == null) {
                        return param;
                    }
                    return param.groupBy(request.getInterval(), request.getFormat());
                })
                .limit(request.getLimit())
                .from(request.getFrom())
                .to(request.getTo())
                .filter(request.getFilter())
                .filter(query -> query.where(ThingsDataConstants.COLUMN_PROPERTY_ID, properties[0].getProperty()))
                .execute(param -> aggregationService.aggregation(metric, param))
                .doOnNext(agg -> agg.remove("_time"))
                .map(AggregationData::of)
                .take(request.getLimit());
        }

        Map<String, String> propertyAlias = context.getPropertyAlias();

        Map<String, PropertyAggregation> aliasProperty = context.getAliasToProperty();

        return AggregationQueryParam
            .of()
            .as(param -> {
                Arrays.stream(properties)
                      .forEach(agg -> param.agg(ThingsDataConstants.COLUMN_PROPERTY_NUMBER_VALUE, "value_" + agg.getAlias(), agg.getAgg(),agg.getDefaultValue()));
                return param;
            })
            .as(param -> {
                if (request.getInterval() == null) {
                    return param;
                }
                return param.groupBy((Group) new TimeGroup(request.getInterval(), "time", request.getFormat()));
            })
            .groupBy(new LimitGroup(ThingsDataConstants.COLUMN_PROPERTY_ID, ThingsDataConstants.COLUMN_PROPERTY_ID, properties.length))
            .limit(request.getLimit() * properties.length)
            .from(request.getFrom())
            .to(request.getTo())
            .filter(request.getFilter())
            .filter(query -> query
                .where()
                .in(ThingsDataConstants.COLUMN_PROPERTY_ID, new HashSet<>(propertyAlias.values())))
            //执行查询
            .execute(param -> aggregationService.aggregation(metric, param))
            .map(AggregationData::of)
            //按时间分组,然后将返回的结果合并起来
            .groupBy(agg -> agg.getString("time", ""), Integer.MAX_VALUE)
            .as(flux -> {
                //按时间分组
                if (request.getInterval() != null) {
                    return flux
                        .flatMap(group -> {
                                     String time = group.key();
                                     return group
                                         //按属性分组
                                         .groupBy(agg -> agg.getString(ThingsDataConstants.COLUMN_PROPERTY_ID, ""), Integer.MAX_VALUE)
                                         .flatMap(propsGroup -> {
                                             String property = propsGroup.key();
                                             return propsGroup
                                                 .reduce(AggregationData::merge)
                                                 .map(agg -> {
                                                     Map<String, Object> data = new HashMap<>();
                                                     data.put("_time", agg.get("_time").orElse(time));
                                                     data.put("time", time);
                                                     aliasProperty.forEach((alias, prp) -> {
                                                         if (prp.getAgg() == Aggregation.FIRST || prp.getAgg() == Aggregation.TOP) {
                                                             data.putIfAbsent(alias, agg
                                                                 .get(ThingsDataConstants.COLUMN_PROPERTY_NUMBER_VALUE)
                                                                 .orElse(agg.get("value").orElse(null)));
                                                         } else if (property.equals(prp.getProperty())) {
                                                             Object value = agg
                                                                 .get("value_" + alias)
                                                                 .orElse(prp.getDefaultValue());
                                                             data.putIfAbsent(alias, value);
                                                         }
                                                     });
                                                     return data;
                                                 });
                                         })
                                         .<Map<String, Object>>reduceWith(HashMap::new, (a, b) -> {
                                             a.putAll(b);
                                             return a;
                                         });
                                 }
                        );
                } else {
                    return flux
                        .flatMap(group -> group
                            .reduce(AggregationData::merge)
                            .map(agg -> {
                                Map<String, Object> values = new HashMap<>();
                                //values.put("time", group.key());
                                for (Map.Entry<String, String> props : propertyAlias.entrySet()) {
                                    PropertyAggregation prop = aliasProperty.get(props.getKey());
                                    values.put(props.getKey(), agg
                                        .get("value_" + props.getKey())
                                        .orElse(prop != null ? prop.getDefaultValue() : 0));
                                }
                                return values;
                            }));
                }
            })
            .map(map -> {
                map.remove("");
                for (Map.Entry<String, String> entry : propertyAlias.entrySet()) {
                    PropertyAggregation agg = aliasProperty.get(entry.getKey());
                    map.putIfAbsent(entry.getKey(), agg != null ? agg.getDefaultValue() : 0);
                }

                return AggregationData.of(map);
            })
            .sort(Comparator
                      .<AggregationData, Date>comparing(agg -> CastUtils.castDate(agg.values().get("_time")))
                      .reversed())
            .doOnNext(agg -> agg.values().remove("_time"))
            .take(request.getLimit())
            ;
    }
}
