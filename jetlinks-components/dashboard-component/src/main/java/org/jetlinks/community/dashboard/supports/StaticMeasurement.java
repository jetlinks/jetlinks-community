package org.jetlinks.community.dashboard.supports;

import lombok.Getter;
import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.MeasurementDefinition;
import org.jetlinks.community.dashboard.MeasurementDimension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaticMeasurement implements Measurement {

    @Getter
    private MeasurementDefinition definition;


    public StaticMeasurement(MeasurementDefinition definition) {
        this.definition = definition;
    }

    private Map<String, MeasurementDimension> dimensions = new ConcurrentHashMap<>();

    public StaticMeasurement addDimension(MeasurementDimension dimension) {

        dimensions.put(dimension.getDefinition().getId(), dimension);

        return this;

    }

    @Override
    public Flux<MeasurementDimension> getDimensions() {
        return Flux.fromIterable(dimensions.values());
    }

    @Override
    public Mono<MeasurementDimension> getDimension(String id) {
        return Mono.justOrEmpty(dimensions.get(id));
    }
}
