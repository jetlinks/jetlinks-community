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
