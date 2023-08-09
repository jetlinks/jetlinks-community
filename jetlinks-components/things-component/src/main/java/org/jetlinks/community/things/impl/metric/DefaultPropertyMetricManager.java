package org.jetlinks.community.things.impl.metric;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.things.Thing;
import org.jetlinks.core.things.ThingId;
import org.jetlinks.core.things.ThingTemplate;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.PropertyMetadataConstants;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.things.impl.entity.PropertyMetricEntity;
import org.jetlinks.community.things.metric.AbstractPropertyMetricManager;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class DefaultPropertyMetricManager extends AbstractPropertyMetricManager {

    private final EventBus eventBus;

    private final ReactiveRepository<PropertyMetricEntity, String> repository;

    public DefaultPropertyMetricManager(ThingsRegistry registry,
                                        EventBus eventBus,
                                        ReactiveRepository<PropertyMetricEntity, String> repository) {
        super(registry);
        this.eventBus = eventBus;
        this.repository = repository;
    }

    public Flux<PropertyMetric> getPropertyMetrics(String thingType,
                                                   String thingId,
                                                   String property) {
        return Mono
            .zip(
                //数据库中记录的
                repository
                    .createQuery()
                    .where(PropertyMetricEntity::getThingType, thingType)
                    .and(PropertyMetricEntity::getThingId, thingId)
                    .and(PropertyMetricEntity::getProperty, property)
                    .fetch()
                    .map(PropertyMetricEntity::toMetric)
                    .collectMap(PropertyMetric::getId),
                //物模型中配置的
                registry
                    .getThing(thingType, thingId)
                    .flatMap(Thing::getTemplate)
                    .flatMap(ThingTemplate::getMetadata)
                    .flatMapIterable(metadata -> metadata
                        .getProperty(property)
                        .map(PropertyMetadataConstants.Metrics::getMetrics)
                        .orElse(Collections.emptyList()))
                    .collectMap(PropertyMetric::getId, Function.identity(), LinkedHashMap::new),
                (exists, inMetadata) -> {
                    for (Map.Entry<String, PropertyMetric> entry : exists.entrySet()) {
                        String metric = entry.getKey();
                        PropertyMetric independent = entry.getValue();
                        PropertyMetric fromMetadata = inMetadata.get(metric);
                        if (fromMetadata == null) {
                            inMetadata.put(metric, independent);
                            continue;
                        }
                        fromMetadata.setValue(independent.getValue());
                    }
                    return Flux.fromIterable(inMetadata.values());
                })
            .flatMapMany(Function.identity());
    }

    public Mono<SaveResult> savePropertyMetrics(String thingType,
                                                String thingId,
                                                String property,
                                                Flux<PropertyMetric> metrics) {
        return metrics
            .map(metric -> {
                PropertyMetricEntity entity = new PropertyMetricEntity();
                entity.setThingId(thingId);
                entity.setThingType(thingType);
                entity.setMetric(metric.getId());
                entity.setMetricName(metric.getName());
                entity.setProperty(property);
                entity.setValue(String.valueOf(metric.getValue()));
                entity.setRange(metric.isRange());
                entity.genericId();
                return entity;
            })
            .as(repository::save);

    }

    @Override
    protected Mono<PropertyMetric> loadPropertyMetric(ThingId thingId,
                                                      String property,
                                                      String metric) {

        return repository
            .findById(PropertyMetricEntity.genericId(thingId.getType(), thingId.getId(), property, metric))
            .map(PropertyMetricEntity::toMetric);
    }

    @EventListener
    public void handleEntityChanged(EntityModifyEvent<PropertyMetricEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(this::handleMetricChangedEvent)
        );
    }

    @EventListener
    public void handleEntityChanged(EntityCreatedEvent<PropertyMetricEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::handleMetricChangedEvent)
        );
    }

    @EventListener
    public void handleEntityChanged(EntityDeletedEvent<PropertyMetricEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::handleMetricChangedEvent)
        );
    }

    @EventListener
    public void handleEntityChanged(EntitySavedEvent<PropertyMetricEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::handleMetricChangedEvent)
        );
    }

    @Subscribe(value = "/_sys/thing-property-metric/clear-cache", features = Subscription.Feature.broker)
    public Mono<Void> handleMetricChangedEvent(CacheKey key) {
        cache.remove(key);
        return Mono.empty();
    }

    private Mono<Void> handleMetricChangedEvent(PropertyMetricEntity entity) {
        CacheKey key = CacheKey.of(ThingId.of(entity.getThingType(), entity.getThingId()), entity.getProperty(), entity.getMetric());
        cache.remove(key);
        return eventBus
            .publish("/_sys/thing-property-metric/clear-cache", key)
            .then();
    }
}
