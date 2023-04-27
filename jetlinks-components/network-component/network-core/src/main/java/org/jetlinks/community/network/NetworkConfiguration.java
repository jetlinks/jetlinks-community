package org.jetlinks.community.network;

import org.jetlinks.community.network.resource.DefaultNetworkResourceUser;
import org.jetlinks.community.network.resource.NetworkResourceManager;
import org.jetlinks.community.network.resource.NetworkResourceUser;
import org.jetlinks.community.network.resource.cluster.DefaultNetworkResourceManager;
import org.jetlinks.community.network.resource.cluster.NetworkResourceProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
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

}
