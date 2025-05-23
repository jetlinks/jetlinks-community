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
    public Mono<NetworkProperties> getConfig(NetworkType networkType, @Nonnull String id) {
        return reactiveRepository
            .findById(id)
            .flatMap(conf -> Mono.justOrEmpty(conf.toNetworkProperties()));
    }
}
