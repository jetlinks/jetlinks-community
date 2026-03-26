package org.jetlinks.community.plugin.impl;

import org.jetlinks.community.plugin.InternalPluginRegistry;
import org.jetlinks.plugin.core.Plugin;
import org.jetlinks.plugin.core.PluginRegistry;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PluginRegistryImpl implements PluginRegistry, SmartInitializingSingleton {
    private final ApplicationContext context;

    private List<InternalPluginRegistry> registries = Collections.emptyList();

    public PluginRegistryImpl(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Mono<Plugin> getPlugin(String type, String pluginId) {
        return Flux
            .fromIterable(registries)
            .flatMap(r -> r.getPlugin(type, pluginId))
            .take(1)
            .singleOrEmpty();
    }

    @Override
    public Flux<Plugin> getPlugins(String type) {
        return Flux
            .fromIterable(registries)
            .flatMap(r -> r.getPlugins(type));
    }

    @Override
    public Flux<Plugin> getPlugins() {
        return Flux
            .fromIterable(registries)
            .flatMap(InternalPluginRegistry::getPlugins);
    }

    @Override
    public void afterSingletonsInstantiated() {
        registries = context
            .getBeanProvider(InternalPluginRegistry.class)
            .stream()
            .collect(Collectors.toList());
    }

}
