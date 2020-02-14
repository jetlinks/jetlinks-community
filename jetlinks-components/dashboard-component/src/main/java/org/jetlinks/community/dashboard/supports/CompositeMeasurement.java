package org.jetlinks.community.dashboard.supports;

import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.MeasurementDefinition;
import org.jetlinks.community.dashboard.MeasurementDimension;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

class CompositeMeasurement implements Measurement {

    private List<Measurement> measurements;

    private Measurement main;

    public CompositeMeasurement(List<Measurement> measurements) {
        Assert.notEmpty(measurements, "measurements can not be empty");
        this.measurements = measurements;
        this.main = measurements.get(0);
    }

    @Override
    public MeasurementDefinition getDefinition() {
        return main.getDefinition();
    }


    @Override
    public Flux<MeasurementDimension> getDimensions() {
        return Flux.fromIterable(measurements)
            .flatMap(Measurement::getDimensions);
    }

    @Override
    public Mono<MeasurementDimension> getDimension(String id) {
        return Flux.fromIterable(measurements)
            .flatMap(measurement -> measurement.getDimension(id))
            .next();
    }
}
