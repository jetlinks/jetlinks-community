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
package org.jetlinks.community.rule.engine.commons.things;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.Values;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.things.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
@Getter
@Setter
public class ThingInfoSpec {

    private Set<String> configs;
    private Set<String> properties;
    private Set<String> tags;
    private Set<String> events;

    public Mono<Map<String, Object>> read(Thing thing, ThingsDataManager dataManager, long time) {
        Map<String, Object> data = Maps.newConcurrentMap();

        Mono<?> task = Mono.empty();
        if (CollectionUtils.isNotEmpty(properties)) {
            task = this
                .readProperties(dataManager, thing, time)
                .doOnNext(v -> data.put("properties", v));
        }

        if (CollectionUtils.isNotEmpty(configs)) {
            task = task
                .then(readConfigs(thing))
                .doOnNext(v -> data.put("configs", v));
        }

        if (CollectionUtils.isNotEmpty(tags)) {
            task = task
                .then(readTags(dataManager, thing, time))
                .doOnNext(v -> data.put("tags", v));
        }
        if (CollectionUtils.isNotEmpty(events)) {
            task = task
                .then(readEvent(dataManager, thing, time))
                .doOnNext(v -> data.put("events", v));
        }

        return task.thenReturn(data);

    }

    private Mono<Map<String, Object>> readConfigs(Thing thing) {

        return thing
            .getSelfConfigs(configs)
            .map(Values::getAllValues);
    }

    private Mono<Map<String, Object>> readEvent(ThingsDataManager dataManager, Thing thing, long baseTime) {
        if (CollectionUtils.isEmpty(events)) {
            return Mono.empty();
        }
        int size = tags.size();
        if (size == 1) {
            return dataManager
                .getLastEvent(thing.getType().getId(), thing.getId(), events.iterator().next(), baseTime)
                .map(p -> converterEvent(p, Maps.newHashMapWithExpectedSize(1)));
        }
        Map<String, Object> tags = Maps.newConcurrentMap();
        return Flux
            .fromIterable(this.tags)
            .flatMap(property -> dataManager
                .getLastEvent(thing.getType().getId(), thing.getId(), property, baseTime)
                .map(p -> converterEvent(p, tags)))
            .then(Mono.just(tags));
    }

    private Mono<Map<String, Object>> readTags(ThingsDataManager dataManager, Thing thing, long baseTime) {
        if (CollectionUtils.isEmpty(tags)) {
            return Mono.empty();
        }
        int size = tags.size();
        if (size == 1) {
            return dataManager
                .getLastTag(thing.getType().getId(), thing.getId(), tags.iterator().next(), baseTime)
                .map(p -> converterTag(p, Maps.newHashMapWithExpectedSize(1)));
        }
        Map<String, Object> tags = Maps.newConcurrentMap();
        return Flux
            .fromIterable(this.tags)
            .flatMap(property -> dataManager
                .getLastTag(thing.getType().getId(), thing.getId(), property, baseTime)
                .map(p -> converterTag(p, tags)))
            .then(Mono.just(tags));
    }

    private Mono<Map<String, Object>> readProperties(ThingsDataManager dataManager, Thing thing, long baseTime) {
        if (CollectionUtils.isEmpty(properties)) {
            return Mono.empty();
        }
        int size = properties.size();
        if (size == 1) {
            return dataManager
                .getLastProperty(thing.getType().getId(), thing.getId(), properties.iterator().next(), baseTime)
                .map(p -> convertProperty(p, Maps.newHashMapWithExpectedSize(1)));
        }
        Map<String, Object> properties = Maps.newConcurrentMap();
        return Flux
            .fromIterable(this.properties)
            .flatMap(property -> dataManager
                .getLastProperty(thing.getType().getId(), thing.getId(), property, baseTime)
                .map(p -> convertProperty(p, properties)))
            .then(Mono.just(properties));
    }

    private Map<String, Object> converterEvent(ThingEvent event, Map<String, Object> container) {
        Map<String, Object> val = Maps.newHashMapWithExpectedSize(3);
        val.put("data", event.getData());
        val.put("timestamp", event.getTimestamp());
        container.put(event.getEvent(), val);
        return container;
    }

    private Map<String, Object> converterTag(ThingTag tag, Map<String, Object> container) {
        Map<String, Object> val = Maps.newHashMapWithExpectedSize(3);
        val.put("value", tag.getValue());
        val.put("timestamp", tag.getTimestamp());
        container.put(tag.getTag(), val);
        return container;
    }

    private Map<String, Object> convertProperty(ThingProperty property, Map<String, Object> container) {
        Map<String, Object> val = Maps.newHashMapWithExpectedSize(3);
        val.put("value", property.getValue());
        val.put("timestamp", property.getTimestamp());
        val.put("state", property.getState());
        container.put(property.getProperty(), val);
        return container;
    }

    protected ObjectType createConfigType() {
        return new ObjectType();
    }

    protected ObjectType createPropertiesType(ThingMetadata metadata) {
        ObjectType type = new ObjectType();
        for (PropertyMetadata tag : metadata.getProperties()) {
            type.addProperty(
                tag.getId(),
                tag.getName(),
                new ObjectType()
                    .addProperty("value",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.property.value", "属性值"),
                                 tag.getValueType())
                    .addProperty("state",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.property.value", "属性状态"),
                                 StringType.GLOBAL)
                    .addProperty("timestamp",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.property.timestamp", "时间戳"),
                                 DateTimeType.GLOBAL)
            );
        }
        return type;
    }

    protected ObjectType createTagsType(ThingMetadata metadata) {
        ObjectType type = new ObjectType();
        for (PropertyMetadata tag : metadata.getTags()) {
            type.addProperty(
                tag.getId(),
                tag.getName(),
                new ObjectType()
                    .addProperty("value",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.tag.value", "标签值"),
                                 tag.getValueType())
                    .addProperty("timestamp",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.tag.timestamp", "时间戳"),
                                 DateTimeType.GLOBAL)
            );
        }
        return type;
    }

    protected ObjectType createEventsType(ThingMetadata metadata) {

        ObjectType type = new ObjectType();
        for (EventMetadata event : metadata.getEvents()) {
            type.addProperty(
                event.getId(),
                event.getName(),
                new ObjectType()
                    .addProperty("data",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.event.data", "时间数据"),
                                 event.getType())
                    .addProperty("timestamp",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.event.timestamp", "时间戳"),
                                 DateTimeType.GLOBAL)
            );
        }
        return type;
    }


    public ObjectType createOutputType(ThingMetadata metadata) {
        ObjectType type = new ObjectType();
        //基本信息
        {
            ObjectType _type = createConfigType();
            if (_type != null && CollectionUtils.isNotEmpty(_type.getProperties())) {
                type.addProperty("configs",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.configs", "基本信息"),
                                 _type);
            }
        }
        //属性
        {
            ObjectType _type = createPropertiesType(metadata);
            if (_type != null && CollectionUtils.isNotEmpty(_type.getProperties())) {
                type.addProperty("properties",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.properties", "物模型属性"),
                                 _type);
            }
        }
        //标签
        {
            ObjectType _type = createTagsType(metadata);
            if (_type != null && CollectionUtils.isNotEmpty(_type.getProperties())) {
                type.addProperty("tags",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.tags", "标签信息"),
                                 _type);
            }
        }
        //事件
        {
            ObjectType _type = createEventsType(metadata);
            if (_type != null && CollectionUtils.isNotEmpty(_type.getProperties())) {
                type.addProperty("events",
                                 LocaleUtils.resolveMessage("message.thing.info.spec.events", "事件信息"),
                                 _type);
            }
        }

        return type;
    }

    public Map<String, Object> toMap() {
        return FastBeanCopier.copy(this, new HashMap<>());
    }
}
