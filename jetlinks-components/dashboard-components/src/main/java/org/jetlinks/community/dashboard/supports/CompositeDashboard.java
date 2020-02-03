package org.jetlinks.community.dashboard.supports;

import lombok.Getter;
import org.jetlinks.community.dashboard.Dashboard;
import org.jetlinks.community.dashboard.DashboardDefinition;
import org.jetlinks.community.dashboard.DashboardObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CompositeDashboard implements Dashboard {

    @Getter
    private DashboardDefinition definition;

    public CompositeDashboard(DashboardDefinition definition) {
        this.definition = definition;
    }

    private Map<String, DashboardObject> staticObjects = new ConcurrentHashMap<>();

    public void addProvider(MeasurementProvider provider) {

        DashboardObject object = staticObjects.computeIfAbsent(provider.getObjectDefinition().getId(), __ -> new CompositeDashboardObject());
        if(object instanceof CompositeDashboardObject){
            CompositeDashboardObject compose = ((CompositeDashboardObject) object);
            compose.addProvider(provider);
        }

    }

    public void addObject(DashboardObject object) {
        staticObjects.put(object.getDefinition().getId(), object);
    }

    @Override
    public Flux<DashboardObject> getObjects() {
        return Flux.fromIterable(staticObjects.values());
    }

    @Override
    public Mono<DashboardObject> getObject(String id) {
        return Mono.justOrEmpty(staticObjects.get(id));
    }
}
