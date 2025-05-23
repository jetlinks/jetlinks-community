package org.jetlinks.community.plugin.context;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.plugin.core.ServiceRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class CompositeServiceRegistry implements ServiceRegistry {
    private final List<ServiceRegistry> registries;

    @Override
    public <T> Optional<T> getService(Class<T> type) {
        for (ServiceRegistry registry : registries) {
            Optional<T> service = registry.getService(type);
            if (service.isPresent()) {
                return service;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(Class<T> type, String name) {
        for (ServiceRegistry registry : registries) {
            Optional<T> service = registry.getService(type, name);
            if (service.isPresent()) {
                return service;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getServices(Class<T> type) {
        for (ServiceRegistry registry : registries) {
            List<T> service = registry.getServices(type);
            if (CollectionUtils.isNotEmpty(service)) {
                return service;
            }
        }
        return Collections.emptyList();
    }
}
