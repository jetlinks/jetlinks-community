package org.jetlinks.community.things.metric;

import org.jetlinks.core.things.ThingId;
import org.jetlinks.community.PropertyMetric;
import reactor.core.publisher.Mono;

public interface PropertyMetricManager {

    Mono<PropertyMetric> getPropertyMetric(ThingId thingId, String property,String metric);

}
