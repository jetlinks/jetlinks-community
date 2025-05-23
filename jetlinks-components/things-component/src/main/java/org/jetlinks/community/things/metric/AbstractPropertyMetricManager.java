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
package org.jetlinks.community.things.metric;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.things.Thing;
import org.jetlinks.core.things.ThingId;
import org.jetlinks.core.things.ThingTemplate;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.PropertyMetadataConstants;
import org.jetlinks.community.PropertyMetric;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public abstract class AbstractPropertyMetricManager implements PropertyMetricManager {

    protected final Map<CacheKey, Mono<PropertyMetric>> cache = new ConcurrentHashMap<>();

    protected final ThingsRegistry registry;

    @Override
    public Mono<PropertyMetric> getPropertyMetric(ThingId thingId, String property, String metric) {

        return cache.computeIfAbsent(CacheKey.of(thingId, property, metric),
                                     key -> this
                                         .loadPropertyMetric(key.thingId, key.property, key.metric)
                                         .cache()
                                         .switchIfEmpty(loadFromTemplate(key.thingId, key.property, key.metric)));
    }

    private Mono<PropertyMetric> loadFromTemplate(ThingId thingId, String property, String metric) {
        return registry
            .getThing(thingId.getType(), thingId.getId())
            .flatMap(Thing::getMetadata)
            .flatMap(metadata -> Mono
                .justOrEmpty(
                    metadata
                        .getProperty(property)
                        .flatMap(propertyMetadata -> PropertyMetadataConstants.Metrics.getMetric(propertyMetadata, metric))
                )
            );
    }

    protected abstract Mono<PropertyMetric> loadPropertyMetric(ThingId thingId, String property, String metric);

    @Getter
    @Setter
    @EqualsAndHashCode
    @AllArgsConstructor(staticName = "of")
    public static class CacheKey implements Serializable {
        private static final long serialVersionUID = 1;

        private final ThingId thingId;
        private final String property;
        private final String metric;
    }
}
