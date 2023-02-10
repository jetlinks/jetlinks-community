package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.network.NetworkConfigManager;
import org.jetlinks.community.network.NetworkProperties;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Component
public class LocalNetworkConfigManager implements NetworkConfigManager {

    private final ReactiveRepository<NetworkConfigEntity, String> reactiveRepository;

    public LocalNetworkConfigManager(ReactiveRepository<NetworkConfigEntity, String> reactiveRepository) {
        this.reactiveRepository = reactiveRepository;
    }

    @Override
    public Flux<NetworkProperties> getAllConfigs() {
        return reactiveRepository
            .createQuery()
            .fetch()
            .flatMap(conf -> Mono.justOrEmpty(conf.toNetworkProperties()));
    }

    @Override
    public Mono<NetworkProperties> getConfig(NetworkType networkType,@Nonnull String id) {
        return reactiveRepository
            .findById(id)
            .flatMap(conf -> Mono.justOrEmpty(conf.toNetworkProperties()));
    }
}
