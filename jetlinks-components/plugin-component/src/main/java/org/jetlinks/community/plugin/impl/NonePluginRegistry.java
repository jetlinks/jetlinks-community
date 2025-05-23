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
