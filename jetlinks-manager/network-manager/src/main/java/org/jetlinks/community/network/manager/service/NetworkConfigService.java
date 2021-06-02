package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.network.NetworkConfigManager;
import org.jetlinks.community.network.NetworkProperties;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0
 **/
@Service
public class NetworkConfigService extends GenericReactiveCrudService<NetworkConfigEntity, String> implements NetworkConfigManager {

    @Override
    public Mono<NetworkProperties> getConfig(NetworkType networkType, String id) {
        return findById(id)
                .map(NetworkConfigEntity::toNetworkProperties);
    }

    @Override
    public Mono<SaveResult> save(Publisher<NetworkConfigEntity> entityPublisher) {
        return super.save(
            Flux.from(entityPublisher)
                .doOnNext(entity -> {
                    if (StringUtils.isEmpty(entity.getId())) {
                        entity.setState(NetworkConfigState.disabled);
                    } else {
                        entity.setState(null);
                    }
                }));
    }

    @Override
    public Mono<Integer> insert(Publisher<NetworkConfigEntity> entityPublisher) {
        return super.insert(
                Flux.from(entityPublisher)
                        .doOnNext(entity -> entity.setState(NetworkConfigState.disabled)));
    }
}
