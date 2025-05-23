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

/**
 * 网络组件，所有网络相关实例根接口
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface Network {

    /**
     * ID唯一标识
     *
     * @return ID
     */
    String getId();

    /**
     * @return 网络类型
     * @see DefaultNetworkType
     */
    NetworkType getType();

    /**
     * 关闭网络组件
     */
    void shutdown();

    /**
     * @return 是否存活
     */
    boolean isAlive();

    /**
     * 当{@link Network#isAlive()}为false是,是否自动重新加载.
     *
     * @return 是否重新加载
     * @see NetworkProvider#reload(Network, Object)
     */
    boolean isAutoReload();
}
