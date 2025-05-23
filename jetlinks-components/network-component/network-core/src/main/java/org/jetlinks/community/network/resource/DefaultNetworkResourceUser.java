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
package org.jetlinks.community.network.resource;

import lombok.AllArgsConstructor;
import org.jetlinks.community.network.NetworkConfigManager;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.ServerNetworkConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@AllArgsConstructor
public class DefaultNetworkResourceUser implements NetworkResourceUser {

    private final NetworkConfigManager configManager;

    private final NetworkManager networkManager;

    @Override
    public Flux<NetworkResource> getUsedResources() {
        return configManager
            .getAllConfigs()
            .flatMap(conf -> networkManager
                .getProvider(conf.getType())
                .map(provider -> provider
                    .createConfig(conf)
                    .onErrorResume(err -> Mono.empty()))
                .orElse(Mono.empty()))
            .filter(ServerNetworkConfig.class::isInstance)
            .cast(ServerNetworkConfig.class)
            .groupBy(ServerNetworkConfig::getHost)
            .flatMap(group -> {
                String host = group.key();
                NetworkResource resource = NetworkResource.of(host);
                return group
                    .doOnNext(conf -> resource.withPorts(conf.getTransport(), Collections.singletonList(conf.getPort())))
                    .then(Mono.just(resource));
            });
    }
}
