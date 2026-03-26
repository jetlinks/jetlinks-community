package org.jetlinks.community.plugin;

import org.jetlinks.plugin.core.Plugin;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InternalPluginRegistry {

    Mono<Plugin> getPlugin(String type, String pluginId);

    Flux<Plugin> getPlugins(String type);

    Flux<Plugin> getPlugins();

}
