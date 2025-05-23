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
package org.jetlinks.community.plugin;

import org.jetlinks.plugin.core.PluginDriver;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 插件驱动管理器
 *
 * @author zhouhao
 * @since 2.0
 */
public interface PluginDriverManager {

    /**
     * 获取全部已加载的驱动信息
     *
     * @return 驱动信息
     */
    Flux<PluginDriver> getDrivers();

    /**
     * 根据ID获取驱动信息
     *
     * @param id ID
     * @return 驱动信息
     */
    Mono<PluginDriver> getDriver(String id);

    /**
     * 监听驱动相关事件,可通过返回值{@link Disposable#dispose()} 来取消监听
     *
     * @param listener 监听器
     * @return Disposable
     */
    Disposable listen(PluginDriverListener listener);
}
