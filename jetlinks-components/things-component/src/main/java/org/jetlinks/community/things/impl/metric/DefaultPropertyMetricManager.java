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
package org.jetlinks.community.things.impl.metric;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
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
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                this::merge)
            .flatMapMany(Function.identity());
    }

    /**
     * 批量获取指标
     *
     * @param thingType  物类型
     * @param thingId    物ID
     * @param properties 属性列表
     * @return 指标信息
     */
    public Flux<DevicePropertyMetricInfo> getPropertyMetrics(String thingType,
                                                             String thingId,
                                                             List<String> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            return Flux.empty();
        }

        //数据库中记录的
        Mono<Map<String, Map<String, PropertyMetric>>> existsByProperty = repository
            .createQuery()
            .where(PropertyMetricEntity::getThingType, thingType)
            .and(PropertyMetricEntity::getThingId, thingId)
            .in(PropertyMetricEntity::getProperty, properties)
            .fetch()
            .groupBy(PropertyMetricEntity::getProperty)
            .flatMap(group -> group
                .map(PropertyMetricEntity::toMetric)
                .collectMap(PropertyMetric::getId, Function.identity())
                .map(map -> Map.entry(group.key(), map)))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue);

        //物模型中配置的
        Mono<Map<String, Map<String, PropertyMetric>>> metadataByProperty = registry
            .getThing(thingType, thingId)
            .flatMap(Thing::getTemplate)
            .flatMap(ThingTemplate::getMetadata)
            .flatMapMany(metadata -> Flux
                .fromIterable(properties)
                .map(prop -> Map.entry(
                    prop,
                    metadata
                        .getProperty(prop)
                        .map(PropertyMetadataConstants.Metrics::getMetrics)
                        .orElse(Collections.emptyList())
                )))
            .flatMap(entry -> Flux
                .fromIterable(entry.getValue())
                .collectMap(PropertyMetric::getId, Function.identity())
                .map(map -> Map.entry(entry.getKey(), map)))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue, LinkedHashMap::new);

        return Mono
            .zip(existsByProperty, metadataByProperty)
            .flatMapMany(tuple -> {
                Map<String, Map<String, PropertyMetric>> exists = tuple.getT1();
                Map<String, Map<String, PropertyMetric>> metadata = tuple.getT2();

                return Flux
                    .fromIterable(properties)
                    .flatMap(prop -> {
                        Map<String, PropertyMetric> existsMetrics = exists.getOrDefault(prop, Collections.emptyMap());
                        Map<String, PropertyMetric> metadataMetrics = new LinkedHashMap<>(metadata.getOrDefault(prop, Collections.emptyMap()));

                        return merge(existsMetrics, metadataMetrics)
                            .collectList()
                            .map(metrics -> new DevicePropertyMetricInfo(prop, metrics));
                    });
            });
    }

    @Getter
    public static class DevicePropertyMetricInfo {
        private final String property;
        private final List<PropertyMetric> metrics;

        public DevicePropertyMetricInfo(String property, List<PropertyMetric> metrics) {
            this.property = property;
            this.metrics = metrics;
        }
    }

    private Flux<PropertyMetric> merge(Map<String, PropertyMetric> exists,
                                       Map<String, PropertyMetric> inMetadata) {
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
    }

    @Transactional
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
            .collectList()
            .flatMap(list -> {
                List<String> ids = list.stream().map(PropertyMetricEntity::getId).collect(Collectors.toList());
                return repository
                    .createDelete()
                    .where(PropertyMetricEntity::getThingType, thingType)
                    .and(PropertyMetricEntity::getThingId, thingId)
                    .and(PropertyMetricEntity::getProperty, property)
                    // 删除当前物模型中的其他指标
                    .when(CollectionUtils.isNotEmpty(ids),
                          delete -> delete.notIn(PropertyMetricEntity::getId, ids))
                    .execute()
                    .then(
                        repository.save(list)
                    );
            });

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