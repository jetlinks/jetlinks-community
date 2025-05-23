package org.jetlinks.community.plugin.context;

import lombok.AllArgsConstructor;
import org.jetlinks.plugin.core.ServiceRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
public class SingleServiceRegistry implements ServiceRegistry {

    private final String name;
    private final Object service;

    @Override
    public <T> Optional<T> getService(Class<T> type) {

        if (type.isInstance(service)) {
            return Optional.of(type.cast(service));
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(Class<T> type, String name) {
        if (Objects.equals(name, this.name)) {
            return getService(type);
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getServices(Class<T> type) {
        if (type.isInstance(service)) {
            return Collections.singletonList(type.cast(service));
        }
        return Collections.emptyList();
    }
}
