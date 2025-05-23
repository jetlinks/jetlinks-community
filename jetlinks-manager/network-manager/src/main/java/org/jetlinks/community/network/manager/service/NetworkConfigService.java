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
package org.jetlinks.community.network.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0
 **/
@Service
public class NetworkConfigService extends GenericReactiveCrudService<NetworkConfigEntity, String> {

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
            .switchIfEmpty(Mono.error(() -> new NotFoundException("error.configuration_does_not_exist",id)))
            .flatMap(conf -> this
                .createUpdate()
                .set(NetworkConfigEntity::getState, NetworkConfigState.enabled)
                .where(conf::getId)
                .execute())
            .then();
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
