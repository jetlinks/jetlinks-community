package org.jetlinks.community.dashboard.supports;

import lombok.Getter;
import org.jetlinks.community.dashboard.Dashboard;
import org.jetlinks.community.dashboard.DashboardDefinition;
import org.jetlinks.community.dashboard.DashboardObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class CompositeDashboard implements Dashboard {

    @Getter
    private DashboardDefinition definition;

    public CompositeDashboard(DashboardDefinition definition) {
        this.definition = definition;
    }

    private Map<String, DashboardObject> staticObjects = new ConcurrentHashMap<>();

    private List<Dashboard> staticDashboard = new CopyOnWriteArrayList<>();

    public void addProvider(MeasurementProvider provider) {

        DashboardObject object = staticObjects.computeIfAbsent(provider.getObjectDefinition().getId(), __ -> new CompositeDashboardObject());
        if(object instanceof CompositeDashboardObject){
            CompositeDashboardObject compose = ((CompositeDashboardObject) object);
            compose.addProvider(provider);
        }

    }

    public void addDashboard(Dashboard dashboard){
        staticDashboard.add(dashboard);
    }

    public void addObject(DashboardObject object) {
        staticObjects.put(object.getDefinition().getId(), object);
    }

    @Override
    public Flux<DashboardObject> getObjects() {
        return Flux.concat(
            Flux.fromIterable(staticObjects.values()),
            Flux.fromIterable(staticDashboard).flatMap(Dashboard::getObjects));
    }

    @Override
    public Mono<DashboardObject> getObject(String id) {
        return Mono.justOrEmpty(staticObjects.get(id))
            .switchIfEmpty(Mono.defer(()-> Flux.fromIterable(staticDashboard)
                .flatMap(dashboard -> dashboard.getObject(id))
                .next()));
    }
}
