package org.jetlinks.community.plugin.impl;

import org.jetlinks.plugin.core.Plugin;
import org.jetlinks.plugin.core.PluginRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class NonePluginRegistry implements PluginRegistry {
    @Override
    public Mono<Plugin> getPlugin(String type, String pluginId) {
        return Mono.empty();
    }

    @Override
    public Flux<Plugin> getPlugins(String type) {
        return Flux.empty();
    }

    @Override
    public Flux<Plugin> getPlugins() {
        return Flux.empty();
    }
}
