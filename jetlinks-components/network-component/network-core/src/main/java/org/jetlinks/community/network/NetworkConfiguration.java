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
package org.jetlinks.community.network;

import org.jetlinks.community.network.resource.DefaultNetworkResourceUser;
import org.jetlinks.community.network.resource.NetworkResourceManager;
import org.jetlinks.community.network.resource.NetworkResourceUser;
import org.jetlinks.community.network.resource.cluster.DefaultNetworkResourceManager;
import org.jetlinks.community.network.resource.cluster.NetworkResourceProperties;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@AutoConfiguration
@EnableConfigurationProperties(NetworkResourceProperties.class)
public class NetworkConfiguration {

    @Bean
    public NetworkResourceManager networkResourceManager(NetworkResourceProperties properties,
                                                         ObjectProvider<NetworkResourceUser> networkResourceUsers) {
        return new DefaultNetworkResourceManager(properties,
                                                 networkResourceUsers
                                                     .stream()
                                                     .collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnBean({
        NetworkConfigManager.class,
        NetworkManager.class
    })
    public NetworkResourceUser defaultNetworkResourceUser(NetworkConfigManager configManager,
                                                          NetworkManager networkManager) {
        return new DefaultNetworkResourceUser(configManager, networkManager);
    }

    @Bean(destroyMethod = "shutdown")
    public ClusterNetworkManager networkManager(EventBus eventBus,
                                                NetworkConfigManager configManager) {
        return new ClusterNetworkManager(eventBus, configManager);
    }

    @Bean
    public CommandLineRunner networkProviderRegister(ObjectProvider<NetworkProvider<?>> providers) {
        for (NetworkProvider<?> provider : providers) {
            NetworkProvider.supports.register(provider.getType().getId(), provider);
        }
        return ignore -> {
        };
    }
}
