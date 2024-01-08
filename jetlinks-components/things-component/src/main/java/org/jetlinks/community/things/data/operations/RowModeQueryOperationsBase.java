package org.jetlinks.community.things.data.operations;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.ThingPropertyDetail;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Function;

public abstract class RowModeQueryOperationsBase extends AbstractQueryOperations {
    public RowModeQueryOperationsBase(String thingType,
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
        Query<?, QueryParamEntity> query = param.toNestQuery(q -> {
            applyQuery(q);
            // property in ('a','b','c')
            q.in(ThingsDataConstants.COLUMN_PROPERTY_ID, properties.keySet());
        });

        return this
            .doQuery(metric, query)
            .mapNotNull(data -> this
                .applyProperty(
                    ThingPropertyDetail
                        .of(data, properties.get(data.getString(ThingsDataConstants.COLUMN_PROPERTY_ID, null))),
                    data.getData()))
            ;
    }

    protected Flux<ThingPropertyDetail> queryEachProperty(@Nonnull String metric,
                                                          @Nonnull Query<?, QueryParamEntity> query,
                                                          @Nonnull ThingMetadata metadata,
                                                          @Nonnull Map<String, PropertyMetadata> properties) {
        return Flux
            .fromIterable(properties.entrySet())
            .flatMap(e -> this
                         .doQuery(metric, query
                             .getParam()
                             .clone()
                             .toQuery()
                             .and(ThingsDataConstants.COLUMN_PROPERTY_ID, e.getKey()))
                         .mapNotNull(data -> this
                             .applyProperty(
                                 ThingPropertyDetail
                                     .of(data, properties.get(data.getString(ThingsDataConstants.COLUMN_PROPERTY_ID, null))),
                                 data.getData())),
                     16);
    }

    protected ThingPropertyDetail applyProperty(ThingPropertyDetail detail, Map<String, Object> data) {
        if (detail == null) {
            return null;
        }
        if (detail.getThingId() == null) {
            detail.thingId((String) data.get(metricBuilder.getThingIdProperty()));
        }
        return detail;
    }

    @Override
    protected final Mono<PagerResult<ThingPropertyDetail>> queryPropertyPage(@Nonnull QueryParamEntity param,
                                                                             @Nonnull ThingMetadata metadata,
                                                                             @Nonnull Map<String, PropertyMetadata> properties) {
        String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId);
        Query<?, QueryParamEntity> query = param.toNestQuery(q -> {
            applyQuery(q);
            // property in ('a','b','c')
            q.in(ThingsDataConstants.COLUMN_PROPERTY_ID, properties.keySet());
        });

        return this
            .doQueryPage(metric,
                         query,
                         data -> this
                             .applyProperty(
                                 ThingPropertyDetail
                                     .of(data, properties.get(data.getString(ThingsDataConstants.COLUMN_PROPERTY_ID, null))),
                                 data.getData()));
    }

    @Override
    protected abstract Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query);

    @Override
    protected abstract <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                                            Query<?, QueryParamEntity> query,
                                                            Function<TimeSeriesData, T> mapper);

    @Override
    protected abstract Flux<AggregationData> doAggregation(String metric, AggregationRequest request, AggregationContext context);
}
