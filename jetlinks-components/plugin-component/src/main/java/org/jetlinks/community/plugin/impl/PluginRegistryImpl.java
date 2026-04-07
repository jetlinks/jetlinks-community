/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
