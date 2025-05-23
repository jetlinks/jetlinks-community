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
