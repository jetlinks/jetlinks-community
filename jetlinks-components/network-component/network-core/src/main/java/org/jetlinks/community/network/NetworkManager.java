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

import java.util.List;
import java.util.Optional;

/**
 * 网络组件管理器，用于统一管理网络组件
 *
 * @author zhouhao
 * @see NetworkProvider
 * @since 1.0
 */
public interface NetworkManager {

    /**
     * 根据组件类型和ID获取网络组件
     *
     * @param type 类型
     * @param id   ID
     * @param <T>  网络组件类型
     * @return Network
     */
    <T extends Network> Mono<T> getNetwork(NetworkType type, String id);

    /**
     * 获取全部网络组件
     *
     * @return 网络组件
     */
    Flux<Network> getNetworks();

    /**
     * 获取全部网络组件提供商
     *
     * @return 提供商列表
     */
    List<NetworkProvider<?>> getProviders();

    /**
     * 根据类型获取提供商
     *
     * @param type 类型
     * @return 提供商
     */
    Optional<NetworkProvider<?>> getProvider(String type);

    /**
     * 重新加载网络组件
     *
     * @param type 网络组件类型
     * @param id   ID
     * @return void
     */
    Mono<Void> reload(NetworkType type, String id);

    /**
     * 停止网络组件
     *
     * @param type 网络组件类型
     * @param id   ID
     * @return void
     */
    Mono<Void> shutdown(NetworkType type, String id);

    /**
     * 销毁网络组件
     *
     * @param type 网络组件类型
     * @param id   ID
     * @return void
     */
    Mono<Void> destroy(NetworkType type, String id);

}
