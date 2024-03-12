package org.jetlinks.community.elastic.search.things;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.things.data.operations.ColumnModeQueryOperationsBase;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.*;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class ElasticSearchColumnModeQueryOperations extends ColumnModeQueryOperationsBase {

    private final ElasticSearchService searchService;
    private final AggregationService aggregationService;

    public ElasticSearchColumnModeQueryOperations(String thingType,
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
                       data.put("timestamp",ts);
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
                            data.put("timestamp",ts);
                            return mapper.apply(TimeSeriesData.of(ts, data));
                        });
    }

    @Override
    protected Flux<AggregationData> doAggregation(String metric, AggregationRequest request, AggregationContext context) {
        org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern(request.getFormat());
        PropertyAggregation[] properties = context.getProperties();
        return AggregationQueryParam
            .of()
            .as(param -> {
                for (PropertyAggregation property : properties) {
                    param.agg(property.getProperty(), property.getAlias(), property.getAgg(),property.getDefaultValue());
                }
                return param;
            })
            .as(param -> {
                if (request.getInterval() == null) {
                    return param;
                }
                return param.groupBy((Group) new TimeGroup(request.getInterval(), "time", request.getFormat()));
            })
            .limit(request.getLimit() * properties.length)
            .from(request.getFrom())
            .to(request.getTo())
            .filter(request.getFilter())
            .execute(param -> aggregationService.aggregation(metric, param))
            .map(AggregationData::of)
            .groupBy(agg -> agg.getString("time", ""), Integer.MAX_VALUE)
            .flatMap(group -> group
                .map(data -> {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("time", data.get("time").orElse(null));
                    for (PropertyAggregation property : properties) {
                        Object val;
                        if(property.getAgg() ==Aggregation.FIRST || property.getAgg()==Aggregation.TOP){
                            val = data
                                .get(property.getProperty())
                                .orElse(null);
                        }else {
                            val = data
                                .get(property.getAlias())
                                .orElse(null);
                        }
                        if (null != val) {
                            newMap.put(property.getAlias(), val);
                        }
                    }
                    return newMap;
                })
                .reduce((a, b) -> {
                    a.putAll(b);
                    return a;
                })
                .map(AggregationData::of))
            .sort(Comparator.<AggregationData, Date>comparing(agg -> DateTime
                .parse(agg.getString("time", ""), formatter)
                .toDate()).reversed())
            .take(request.getLimit())
            ;
    }
}
