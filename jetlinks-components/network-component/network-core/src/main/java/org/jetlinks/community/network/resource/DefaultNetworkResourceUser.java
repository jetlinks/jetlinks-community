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
