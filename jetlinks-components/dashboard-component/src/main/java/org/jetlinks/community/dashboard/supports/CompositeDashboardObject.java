package org.jetlinks.community.dashboard.supports;

import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.community.dashboard.DashboardObject;
import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.ObjectDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class CompositeDashboardObject implements DashboardObject {

    private ObjectDefinition definition;

    private List<MeasurementProvider> providers = new CopyOnWriteArrayList<>();

    public void addProvider(MeasurementProvider provider) {
        if (definition == null) {
            definition = provider.getObjectDefinition();
        }
        providers.add(provider);
    }

    @Override
    public ObjectDefinition getDefinition() {
        return definition;
    }

    @Override
    public Flux<Measurement> getMeasurements() {
        return Flux.fromIterable(providers)
            .flatMap(MeasurementProvider::getMeasurements);
    }

    @Override
    public Mono<Measurement> getMeasurement(String id) {
        return Flux.fromIterable(providers)
            .flatMap(provider -> provider.getMeasurement(id))
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .map(CompositeMeasurement::new);
    }
}
