package org.jetlinks.community.things.data.operations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.Thing;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingTemplate;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.*;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
public abstract class AbstractQueryOperations implements QueryOperations {

    protected final String thingType;
    protected final String thingTemplateId;
    protected final String thingId;
    protected final MetricBuilder metricBuilder;
    protected final DataSettings settings;
    protected final ThingsRegistry registry;


    protected abstract Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query);

    protected abstract <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                                            Query<?, QueryParamEntity> query,
                                                            Function<TimeSeriesData, T> mapper);

    protected abstract Flux<AggregationData> doAggregation(String metric, AggregationRequest request, AggregationContext context);


    protected Mono<ThingMetadata> getMetadata() {
        //指定了物实例
        if (StringUtils.hasText(thingId)) {
            return registry
                .getThing(thingType, thingId)
                .flatMap(Thing::getMetadata);
        }
        return registry
            .getTemplate(thingType, thingTemplateId)
            .flatMap(ThingTemplate::getMetadata);
    }

    protected void applyQuery(Query<?, ? extends QueryParam> query) {

        //默认时间戳倒序
        if (CollectionUtils.isEmpty(query.getParam().getSorts())) {
            query.orderByDesc(ThingsDataConstants.COLUMN_TIMESTAMP);
        }
        //指定物实例，追加条件: where thingId = ?
        if (StringUtils.hasText(thingId)) {
            query.and(metricBuilder.getThingIdProperty(), thingId);
        }

    }


    protected abstract Flux<ThingPropertyDetail> queryProperty(@Nonnull QueryParamEntity param,
                                                               @Nonnull ThingMetadata metadata,
                                                               @Nonnull Map<String, PropertyMetadata> properties);

    protected abstract Mono<PagerResult<ThingPropertyDetail>> queryPropertyPage(@Nonnull QueryParamEntity param,
                                                                                @Nonnull ThingMetadata metadata,
                                                                                @Nonnull Map<String, PropertyMetadata> properties);


    protected abstract Flux<ThingPropertyDetail> queryEachProperty(@Nonnull String metric,
                                                                   @Nonnull Query<?, QueryParamEntity> query,
                                                                   @Nonnull ThingMetadata metadata,
                                                                   @Nonnull Map<String, PropertyMetadata> properties);

    @Nonnull
    @Override
    public final Flux<ThingPropertyDetail> queryEachProperty(@Nonnull QueryParamEntity param,
                                                             @Nonnull String... property) {

        return this
            .getMetadata()
            .flatMapMany(metadata -> {
                Map<String, PropertyMetadata> properties = getProperties(metadata, property);
                if (properties.isEmpty()) {
                    return Mono.empty();
                }
                if (properties.size() == 1) {
                    return queryProperty(param, metadata, properties);
                }
                String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId);
                Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);
                return queryEachProperty(metric, query, metadata, properties);
            });
    }

    @Nonnull
    @Override
    public final Flux<ThingPropertyDetail> queryProperty(@Nonnull QueryParamEntity query,
                                                         @Nonnull String... property) {


        return this
            .getMetadata()
            .flatMapMany(metadata -> {
                Map<String, PropertyMetadata> properties = getProperties(metadata, property);
                if (properties.isEmpty()) {
                    return Mono.empty();
                }
                return queryProperty(query.clone(), metadata, properties);
            });
    }

    @Nonnull
    @Override
    public final Mono<PagerResult<ThingPropertyDetail>> queryPropertyPage(@Nonnull QueryParamEntity query,
                                                                          @Nonnull String... property) {
        return this
            .getMetadata()
            .flatMap(metadata -> {
                Map<String, PropertyMetadata> properties = getProperties(metadata, property);
                if (properties.isEmpty()) {
                    return Mono.empty();
                }
                return queryPropertyPage(query.clone(), metadata, properties);
            })
            .defaultIfEmpty(PagerResult.of(0, new ArrayList<>(), query));
    }

    private Map<String, PropertyMetadata> getProperties(ThingMetadata metadata, String... property) {
        if (property.length == 0) {
            return metadata
                .getProperties()
                .stream()
                .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity()));
        }
        Set<String> propertiesFilter = property.length == 1
            ? Collections.singleton(property[0])
            : new HashSet<>(Arrays.asList(property));
        return metadata
            .getProperties()
            .stream()
            .filter(p -> propertiesFilter.contains(p.getId()))
            .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity()));
    }

    @Nonnull
    @Override
    public final Flux<AggregationData> aggregationProperties(@Nonnull AggregationRequest request,
                                                             @Nonnull PropertyAggregation... properties) {
        String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId);

        AggregationRequest aggRequest = request.copy();

        aggRequest.getFilter().toNestQuery(this::applyQuery);

        return getMetadata()
            .flatMapMany(metadata -> doAggregation(metric, aggRequest, new AggregationContext(metadata, properties)));
    }


    @Override
    public Flux<ThingMessageLog> queryMessageLog(@Nonnull QueryParamEntity param) {
        String metric = metricBuilder.createLogMetric(thingType, thingTemplateId, thingId);
        Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);
        return doQuery(metric, query)
            .map(data -> ThingMessageLog.of(data, metricBuilder.getThingIdProperty()));
    }

    @Override
    public Mono<PagerResult<ThingMessageLog>> queryMessageLogPage(@Nonnull QueryParamEntity param) {
        String metric = metricBuilder.createLogMetric(thingType, thingTemplateId, thingId);
        Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);
        return doQueryPage(metric, query, data -> ThingMessageLog.of(data, metricBuilder.getThingIdProperty()));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<ThingEvent>> queryEventPage(@Nonnull String eventId,
                                                        @Nonnull QueryParamEntity param,
                                                        boolean format) {
        Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);
        String metric;
        if (settings.getEvent().eventIsAllInOne()) {
            metric = metricBuilder.createEventAllInOneMetric(thingType, thingTemplateId, thingId);
            query.and(ThingsDataConstants.COLUMN_EVENT_ID, eventId);
        } else {
            metric = metricBuilder.createEventMetric(thingType, thingTemplateId, thingId, eventId);
        }
        if (format) {
            return getMetadata()
                .mapNotNull(metadata -> metadata.getEventOrNull(eventId))
                .flatMap(metadata -> doQueryPage(metric, query, data -> ThingEvent
                    .of(data, metricBuilder.getThingIdProperty())
                    .putFormat(metadata)))
                .defaultIfEmpty(PagerResult.of(0, new ArrayList<>(), param));
        }
        return doQueryPage(metric, query, data -> ThingEvent.of(data, metricBuilder.getThingIdProperty()))
            .defaultIfEmpty(PagerResult.of(0, new ArrayList<>(), param));
    }

    @Nonnull
    @Override
    public final Flux<ThingEvent> queryEvent(@Nonnull String eventId, @Nonnull QueryParamEntity param, boolean format) {
        Query<?, QueryParamEntity> query = param.toNestQuery(this::applyQuery);
        String metric;
        if (settings.getEvent().eventIsAllInOne()) {
            metric = metricBuilder.createEventAllInOneMetric(thingType, thingTemplateId, thingId);
            query.and(ThingsDataConstants.COLUMN_EVENT_ID, eventId);
        } else {
            metric = metricBuilder.createEventMetric(thingType, thingTemplateId, thingId, eventId);
        }
        if (format) {
            return getMetadata()
                .mapNotNull(metadata -> metadata.getEventOrNull(eventId))
                .flatMapMany(metadata -> doQuery(metric, query).map(data -> ThingEvent
                    .of(data, metricBuilder.getThingIdProperty())
                    .putFormat(metadata)));
        }
        return doQuery(metric, query)
            .map(data -> ThingEvent.of(data, metricBuilder.getThingIdProperty()));
    }

    @Getter
    protected static class AggregationContext {

        private final Map<String, String> propertyAlias;
        private final Map<String, PropertyAggregation> aliasToProperty;

        private final PropertyAggregation[] properties;
        private final ThingMetadata metadata;

        public AggregationContext(ThingMetadata metadata, PropertyAggregation... properties) {
            this.metadata = metadata;
            this.properties = properties;
            propertyAlias = Arrays
                .stream(properties)
                .collect(Collectors.toMap(PropertyAggregation::getAlias,
                                          PropertyAggregation::getProperty));
            aliasToProperty = Arrays
                .stream(properties)
                .collect(Collectors.toMap(PropertyAggregation::getAlias,
                                          Function.identity()));
        }
    }
}
