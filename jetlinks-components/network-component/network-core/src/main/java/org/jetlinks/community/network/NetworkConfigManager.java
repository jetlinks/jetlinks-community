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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 网络组件配置管理器
 *
 * @author zhouhao
 * @since 1.0
 */
public interface NetworkConfigManager {

    /**
     * 获取全部的网络配置
     *
     * @return 配置信息
     * @since 2.0
     */
    default Flux<NetworkProperties> getAllConfigs(boolean selfServer) {
        return getAllConfigs();
    }

    /**
     * 获取全部的网络配置
     *
     * @return 配置信息
     * @since 2.0
     */
    default Flux<NetworkProperties> getAllConfigs() {
        return Flux.empty();
    }

    /**
     * 根据网络类型和配置ID获取配置信息
     *
     * @param networkType 网络类型
     * @param id          配置ID
     * @param selfServer  是否只获取当前集群节点的配置
     * @return 配置信息
     */
    default Flux<NetworkProperties> getConfig(
        @Nullable NetworkType networkType,
        @Nonnull String id,
        boolean selfServer) {
        return getConfig(networkType, id).flux();
    }

    /**
     * 根据网络类型和配置ID获取配置信息
     *
     * @param networkType 网络类型
     * @param id          配置ID
     * @return 配置信息
     */
    Mono<NetworkProperties> getConfig(@Nullable NetworkType networkType,
                                      @Nonnull String id);

}
