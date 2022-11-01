package org.jetlinks.community.things.data.operations;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.ThingProperties;
import org.jetlinks.community.things.data.ThingPropertyDetail;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Function;

/**
 * 列式模式查询操作基础类,实现列式模式下通用查询相关操作.
 * <p>
 * 列式模式表示: 属性相关消息
 *
 * @author zhouhao
 * @since 2.0
 */
public abstract class ColumnModeQueryOperationsBase extends AbstractQueryOperations implements ColumnModeQueryOperations {

    public ColumnModeQueryOperationsBase(String thingType,
                                         String thingTemplateId,
                                         String thingId,
                                         MetricBuilder metricBuilder,
                                         DataSettings settings,
                                         ThingsRegistry registry) {
        super(thingType, thingTemplateId, thingId, metricBuilder, settings, registry);
    }

    @Override
    protected final Flux<ThingPropertyDetail> queryProperty(@Nonnull QueryParamEntity param,
                                                            @Nonnull ThingMetadata metadata,
                                                            @Nonnull Map<String, PropertyMetadata> properties) {
        String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId);
        return queryProperty(metric, param.toNestQuery(this::applyQuery), metadata, properties);
    }

    protected Flux<ThingPropertyDetail> queryProperty(@Nonnull String metric,
                                                      @Nonnull Query<?, QueryParamEntity> query,
                                                      @Nonnull ThingMetadata metadata,
                                                      @Nonnull Map<String, PropertyMetadata> properties) {
        return this
            .doQuery(metric, query)
            .flatMap(data -> Flux
                .create(sink -> {
                    for (Map.Entry<String, PropertyMetadata> entry : properties.entrySet()) {
                        data
                            .get(entry.getKey())
                            .ifPresent(value -> sink
                                .next(ThingPropertyDetail
                                          .of(value, entry.getValue())
                                          .thingId(data.getString(metricBuilder.getThingIdProperty(), null))
                                          .timestamp(data.getTimestamp())
                                          .createTime(data.getLong(ThingsDataConstants.COLUMN_CREATE_TIME,data.getTimestamp()))
                                          .generateId()
                                ));
                    }
                    sink.complete();
                }));
    }

    protected Flux<ThingPropertyDetail> queryEachProperty(@Nonnull String metric,
                                                          @Nonnull Query<?, QueryParamEntity> query,
                                                          @Nonnull ThingMetadata metadata,
                                                          @Nonnull Map<String, PropertyMetadata> properties) {
        return queryProperty(metric, query, metadata, properties);
    }

    @Override
    protected Mono<PagerResult<ThingPropertyDetail>> queryPropertyPage(@Nonnull QueryParamEntity param,
                                                                       @Nonnull ThingMetadata metadata,
                                                                       @Nonnull Map<String, PropertyMetadata> properties) {

        if (properties.size() > 1) {
            //列式模式不支持同时查询多个属性
            return Mono.error(new UnsupportedOperationException("error.unsupported_query_multi_property"));
        }
        String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId);
        Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);
        String property = properties.keySet().iterator().next();

        query.notNull(property);
        return this
            .doQueryPage(metric,
                         query,
                         data -> ThingPropertyDetail
                             .of(data.get(property).orElse(null), properties.get(property))
                             .thingId(data.getString(metricBuilder.getThingIdProperty(), null))
                             .timestamp(data.getTimestamp())
                             .createTime(data.getLong(ThingsDataConstants.COLUMN_CREATE_TIME, data.getTimestamp()))
                             .generateId()
            );
    }

    @Nonnull
    @Override
    public Mono<PagerResult<ThingProperties>> queryAllPropertiesPage(@Nonnull QueryParamEntity param) {
        String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId);
        Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);

        return doQueryPage(metric, query, data -> new ThingProperties(data.getData(), metricBuilder.getThingIdProperty()));
    }

    @Nonnull
    @Override
    public Flux<ThingProperties> queryAllProperties(@Nonnull QueryParamEntity param) {
        String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId);
        Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);

        return doQuery(metric, query)
            .map(data -> new ThingProperties(data.getData(), metricBuilder.getThingIdProperty()));
    }

    @Override
    protected abstract Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query);

    @Override
    protected abstract <T> Mono<PagerResult<T>> doQueryPage(String metric, Query<?, QueryParamEntity> query, Function<TimeSeriesData, T> mapper);

    @Override
    protected abstract Flux<AggregationData> doAggregation(String metric, AggregationRequest request, AggregationContext context);
}
