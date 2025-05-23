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

import java.util.List;
import java.util.Optional;

/**
 * 网络组件类型，通常使用枚举实现
 *
 * @author zhouhao
 * @see DefaultNetworkType
 * @since 1.0
 */
public interface NetworkType {
    /**
     * @return 类型唯一标识
     */
    String getId();

    /**
     * @return 类型名称
     */
    default String getName() {
        return getId();
    }

    /**
     * 使用指定的ID创建一个NetworkType
     *
     * @param id ID
     * @return NetworkType
     */
    static NetworkType of(String id) {
        return () -> id;
    }

    /**
     * 获取所有支持的网络组件类型
     *
     * @return 所有支持的网络组件类型
     */
    static List<NetworkType> getAll() {
        return NetworkTypes.get();
    }

    /**
     * 根据网络组件类型ID获取类型对象
     *
     * @param id ID
     * @return Optional
     */
    static Optional<NetworkType> lookup(String id) {
        return NetworkTypes.lookup(id);
    }
}
