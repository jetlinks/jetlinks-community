package org.jetlinks.community.tdengine.things;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.things.data.ThingPropertyDetail;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeQueryOperationsBase;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

class TDengineRowModeQueryOperations extends RowModeQueryOperationsBase {

    final TDengineThingDataHelper helper;

    public TDengineRowModeQueryOperations(String thingType,
                                          String thingTemplateId,
                                          String thingId,
                                          MetricBuilder metricBuilder,
                                          DataSettings settings,
                                          ThingsRegistry registry,
                                          TDengineThingDataHelper helper) {
        super(thingType, thingTemplateId, thingId, metricBuilder, settings, registry);
        this.helper = helper;
    }

    @Override
    protected Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query) {
        return helper.doQuery(metric, query);
    }

    @Override
    protected <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                                   Query<?, QueryParamEntity> query,
                                                   Function<TimeSeriesData, T> mapper) {
        return helper.doQueryPage(metric, query, mapper);
    }

    @Override
    protected Flux<ThingPropertyDetail> queryEachProperty(@Nonnull String metric,
                                                          @Nonnull Query<?, QueryParamEntity> query,
                                                          @Nonnull ThingMetadata metadata,
                                                          @Nonnull Map<String, PropertyMetadata> properties) {
        return super.queryEachProperty(metric,query,metadata,properties);
    }

    @Override
    protected Flux<AggregationData> doAggregation(String metric,
                                                  AggregationRequest request,
                                                  AggregationContext context) {
        PropertyAggregation[] properties = context.getProperties();


        //聚合
        StringJoiner agg = new StringJoiner("");
        agg.add("property,last(`_ts`) _ts");

        for (PropertyAggregation property : properties) {
            agg.add(",");
            agg.add(TDengineThingDataHelper.convertAggFunction(property));
            if(property.getAgg()== Aggregation.COUNT){
                agg .add("(`value`)");
            }else {
                agg .add("(`numberValue`)");
            }
            agg.add(" `").add("value_" + property.getAlias()).add("`");
        }

        String sql = String.join(
            "",
            "`", metric, "` ",
            helper.buildWhere(metric,
                              request
                                  .getFilter()
                                  .clone()
                                  .and("property", TermType.in, context.getPropertyAlias().values())
                                  .and("_ts", TermType.btw, Arrays.asList(request.getFrom(), request.getTo()))
            )
        );
        String dataSql = "select " + agg + " from " + sql + " partition by property";
        if (request.getInterval() != null) {
            dataSql += " ";
            dataSql += TDengineThingDataHelper.getGroupByTime(request.getInterval());
        }
        String format = request.getFormat();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        if (properties.length == 1) {
            String key = "value_" + properties[0].getAlias();
            return helper
                .query(dataSql)
                .sort(Comparator.comparing(TimeSeriesData::getTimestamp).reversed())
                .map(timeSeriesData -> {
                    long ts = timeSeriesData.getTimestamp();
                    Map<String, Object> newData = new HashMap<>();
                    newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId
                        .systemDefault())));
                    newData.put(properties[0].getAlias(), timeSeriesData.get(key).orElse(properties[0].getDefaultValue()));

                    return AggregationData.of(newData);
                })
                .take(request.getLimit())
                ;
        }
        return helper
            .query(dataSql)
            .map(timeSeriesData -> {
                long ts = timeSeriesData.getTimestamp();
                Map<String, Object> newData = timeSeriesData.getData();
                newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())));
                newData.put("_time", ts);
                return newData;
            })
            .groupBy(data -> (String) data.get("time"), Integer.MAX_VALUE)
            .flatMap(group -> group
                .reduceWith(HashMap::new, (a, b) -> {
                    a.putAll(b);
                    return a;
                })
                .map(map -> {
                    Map<String, Object> newResult = new HashMap<>();
                    for (PropertyAggregation property : properties) {
                        String key = "value_" + property.getAlias();
                        newResult.put(property.getAlias(), Optional.ofNullable(map.get(key)).orElse(property.getDefaultValue()));
                    }
                    newResult.put("time", group.key());
                    newResult.put("_time", map.getOrDefault("_time", new Date()));
                    return AggregationData.of(newResult);
                }))
            .sort(Comparator
                      .<AggregationData, Date>comparing(data -> CastUtils.castDate(data.values().get("_time")))
                      .reversed())
            .doOnNext(data -> data.values().remove("_time"))
            .take(request.getLimit());
    }
}
