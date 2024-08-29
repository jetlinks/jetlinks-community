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
