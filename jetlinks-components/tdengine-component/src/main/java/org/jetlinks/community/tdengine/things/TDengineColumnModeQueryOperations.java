package org.jetlinks.community.tdengine.things;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.things.data.operations.ColumnModeQueryOperationsBase;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;


class TDengineColumnModeQueryOperations extends ColumnModeQueryOperationsBase {

    final TDengineThingDataHelper helper;

    public TDengineColumnModeQueryOperations(String thingType,
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
    protected Flux<AggregationData> doAggregation(String metric,
                                                  AggregationRequest request,
                                                  AggregationContext context) {
        StringJoiner joiner = new StringJoiner("", "select ", "");
        joiner.add("last(`_ts`) _ts");

        for (PropertyAggregation property : context.getProperties()) {
            joiner.add(",");
            joiner.add(TDengineThingDataHelper.convertAggFunction(property))
                  .add("(`").add(property.getProperty()).add("`)")
                  .add(" `").add(property.getAlias()).add("`");
        }

        joiner
            .add(" from `").add(metric).add("` ")
            .add(helper.buildWhere(
                metric,
                request.getFilter().clone().and("_ts", "btw", Arrays.asList(request.getFrom(), request.getTo())))
            );


        if (request.getInterval() != null) {
            joiner.add(" ")
                  .add(TDengineThingDataHelper.getGroupByTime(request.getInterval()));
        }
        joiner.add(helper.buildOrderBy(metric, request.getFilter()));

        String format = request.getFormat();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        return helper
            .operations
            .forQuery()
            .query(joiner.toString(), ResultWrappers.map())
            .map(map -> {
                TimeSeriesData timeSeriesData = TDengineThingDataHelper.convertToTsData(map);
                long ts = timeSeriesData.getTimestamp();
                Map<String, Object> newData = timeSeriesData.getData();
                for (PropertyAggregation property : context.getProperties()) {
                    newData.putIfAbsent(property.getAlias(), property.getDefaultValue());
                }
                newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())));
                return AggregationData.of(newData);
            })
            .take(request.getLimit());
    }
}
