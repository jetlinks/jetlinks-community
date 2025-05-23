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

import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.community.plugin.PluginDriverConfig;
import org.jetlinks.community.plugin.PluginDriverInstaller;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPluginDriverInstaller implements PluginDriverInstaller {

    private final Map<String, PluginDriverInstallerProvider> providers = new ConcurrentHashMap<>();

    public void addProvider(PluginDriverInstallerProvider provider) {
        providers.put(provider.provider(), provider);
    }

    @Override
    public Mono<PluginDriver> install(PluginDriverConfig config) {
        PluginDriverInstallerProvider provider = providers.get(config.getProvider());
        if (null == provider) {
            return Mono.error(() -> new UnsupportedOperationException("unsupported plugin provider:" + config.getProvider()));
        }
        return provider.install(config);
    }

    @Override
    public Mono<PluginDriver> reload(PluginDriver driver, PluginDriverConfig config) {
        PluginDriverInstallerProvider provider = providers.get(config.getProvider());
        if (null == provider) {
            return Mono.error(() -> new UnsupportedOperationException("unsupported plugin provider:" + config.getProvider()));
        }
        return provider.reload(driver,config);
    }

    @Override
    public Mono<Void> uninstall(PluginDriverConfig config) {
        PluginDriverInstallerProvider provider = providers.get(config.getProvider());
        if (provider == null) {
            return Mono.empty();
        }
        return provider.uninstall(config);
    }
}
