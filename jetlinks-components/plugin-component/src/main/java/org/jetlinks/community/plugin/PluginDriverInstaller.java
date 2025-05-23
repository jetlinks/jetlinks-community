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
import org.jetlinks.community.plugin.impl.PluginDriverInstallerProvider;
import reactor.core.publisher.Mono;

/**
 * 插件驱动安装器,用于根据查看驱动配置安装,卸载插件驱动
 *
 * @author zhouhao
 * @see PluginDriverInstallerProvider
 * @since 2.0
 */
public interface PluginDriverInstaller {

    /**
     * 安装驱动
     *
     * @param config 驱动配置
     * @return 驱动
     */
    Mono<PluginDriver> install(PluginDriverConfig config);

    /**
     * 重新加载驱动
     * @param driver 旧驱动
     * @param config 驱动配置
     * @return 驱动
     */
    Mono<PluginDriver> reload(PluginDriver driver,
                              PluginDriverConfig config);

    /**
     * 卸载驱动
     *
     * @param config 驱动配置
     * @return void
     */
    Mono<Void> uninstall(PluginDriverConfig config);

}
