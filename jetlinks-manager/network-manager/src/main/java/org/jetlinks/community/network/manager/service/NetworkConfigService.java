package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.NetworkConfigManager;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkProperties;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0
 **/
@Service
public class NetworkConfigService extends GenericReactiveCrudService<NetworkConfigEntity, String>  {

    private final NetworkManager networkManager;

    public NetworkConfigService(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return this
            .findById(Flux.from(idPublisher))
            .flatMap(config -> networkManager
                .destroy(config.lookupNetworkType(), config.getId())
                .thenReturn(config.getId()))
            .as(super::deleteById)
            ;
    }


    public Mono<Void> start(String id) {
        return this
            .findById(id)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("error.configuration_does_not_exist", id)))
            .flatMap(conf -> this
                .createUpdate()
                .set(NetworkConfigEntity::getState, NetworkConfigState.enabled)
                .where(conf::getId)
                .execute()
                .thenReturn(conf))
            .flatMap(conf -> networkManager.reload(conf.lookupNetworkType(), id));
    }

    public Mono<Void> shutdown(String id) {
        return this
            .findById(id)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("error.configuration_does_not_exist",id)))
            .flatMap(conf -> this
                .createUpdate()
                .set(NetworkConfigEntity::getState, NetworkConfigState.disabled)
                .where(conf::getId)
                .execute()
                .thenReturn(conf))
            .flatMap(conf -> networkManager.shutdown(conf.lookupNetworkType(), id));
    }
}
