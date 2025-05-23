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

import org.jetlinks.community.dashboard.Dashboard;
import org.jetlinks.community.dashboard.DashboardDefinition;
import org.jetlinks.community.dashboard.DashboardManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultDashboardManager implements DashboardManager, BeanPostProcessor {

    private Map<String, CompositeDashboard> dashboards = new ConcurrentHashMap<>();

    @Override
    public Flux<Dashboard> getDashboards() {
        return Flux.fromIterable(dashboards.values());
    }

    @Override
    public Mono<Dashboard> getDashboard(String id) {
        return Mono.justOrEmpty(dashboards.get(id));
    }


    private void addProvider(MeasurementProvider provider) {

        DashboardDefinition definition = provider.getDashboardDefinition();

        CompositeDashboard dashboard = dashboards.computeIfAbsent(definition.getId(), __ -> new CompositeDashboard(definition));

        dashboard.addProvider(provider);
    }

    private void addDashboard(Dashboard dashboard) {

        CompositeDashboard cached = dashboards.computeIfAbsent(dashboard.getDefinition().getId(), __ -> new CompositeDashboard(dashboard.getDefinition()));

        cached.addDashboard(dashboard);

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof MeasurementProvider) {
            addProvider(((MeasurementProvider) bean));
        } else if (bean instanceof Dashboard) {
            addDashboard(((Dashboard) bean));
        }
        return bean;
    }
}
