package org.jetlinks.community.dashboard.supports;

import org.jetlinks.community.dashboard.DashboardDefinition;
import org.jetlinks.community.dashboard.DashboardManager;
import org.jetlinks.community.dashboard.Dashboard;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultDashboardManager implements DashboardManager, BeanPostProcessor {

    private Map<String, Dashboard> dashboards = new ConcurrentHashMap<>();

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

        Dashboard dashboard = dashboards.computeIfAbsent(definition.getId(), __ -> new CompositeDashboard(definition));

        if (dashboard instanceof CompositeDashboard) {
            CompositeDashboard compose = ((CompositeDashboard) dashboard);
            compose.addProvider(provider);
        } else {
            throw new UnsupportedOperationException("unsupported register dashboard object : " + provider);
        }
    }

    private void addDashboard(Dashboard dashboard) {
        dashboards.put(dashboard.getDefinition().getId(), dashboard);
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
