/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
import org.jetlinks.community.things.data.ThingsDataUtils;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeQueryOperationsBase;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.utils.SqlSecurityUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        // SQL 只使用服务端生成的别名，查询后再映射回请求 alias。
        Map<String, String> aliases = new LinkedHashMap<>();
        int index = 0;
        for (PropertyAggregation property : properties) {
            String alias = property.getAlias();
            String internalAlias = SqlSecurityUtils.aggregationAlias(index++);
            aliases.put(alias, internalAlias);
            agg.add(",").add(createAggregationColumn(property, internalAlias));
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
            String alias = properties[0].getAlias();
            String key = aliases.get(alias);
            return helper
                .query(dataSql)
                .sort(Comparator.comparing(TimeSeriesData::getTimestamp).reversed())
                .map(timeSeriesData -> {
                    long ts = timeSeriesData.getTimestamp();
                    Map<String, Object> newData = new HashMap<>();
                    newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId
                        .systemDefault())));
                    newData.put(alias, timeSeriesData.get(key).orElse(properties[0].getDefaultValue()));

                    return AggregationData.of(newData);
                })
                .take(request.getLimit())
                ;
        }
        NavigableMap<Long, Map<String, Object>> prepares =
            ThingsDataUtils.prepareAggregationData(request, properties);
        Map<String, List<PropertyAggregation>> propertyAgg = Arrays
            .stream(properties)
            .collect(Collectors.groupingBy(PropertyAggregation::getProperty));
        return helper
            .query(dataSql)
            .doOnNext(data -> {
                long timestamp = data.getTimestamp();
                Map<String, Object> prepare = ThingsDataUtils.findAggregationData(timestamp, prepares);
                if (prepare != null) {
                    Object propertyValue = data.getData().get("property");
                    List<PropertyAggregation> proAggs = propertyValue == null
                        ? null
                        : propertyAgg.get(propertyValue.toString());
                    // 每个 partition 行都会计算全部投影，只消费当前 property 对应的聚合列。
                    if (proAggs != null) {
                        for (PropertyAggregation proAgg : proAggs) {
                            String alias = proAgg.getAlias();
                            prepare.put(alias, data.get(aliases.get(alias)).orElse(proAgg.getDefaultValue()));
                        }
                    }
                }
            })
            .thenMany(Flux.fromIterable(prepares.descendingMap().values()))
            .map(AggregationData::of)
            .take(request.getLimit());
    }

    static String createAggregationColumn(PropertyAggregation property, String internalAlias) {
        String valueColumn = property.getAgg() == Aggregation.COUNT ? "value" : "numberValue";
        return TDengineThingDataHelper.convertAggFunction(property)
            + "(" + SqlSecurityUtils.quoteBacktick(valueColumn) + ") "
            + SqlSecurityUtils.quoteBacktick(internalAlias);
    }
}
