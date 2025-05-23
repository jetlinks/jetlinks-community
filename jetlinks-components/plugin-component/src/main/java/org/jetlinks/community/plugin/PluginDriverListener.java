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
import reactor.core.publisher.Mono;

/**
 * 插件驱动监听器
 *
 * @author zhouhao
 * @since 2.1
 */
public interface PluginDriverListener {

    /**
     * 当插件安装时触发
     *
     * @param driverId 驱动ID
     * @param driver   驱动
     * @return void
     */
    default Mono<Void> onInstall(String driverId,
                                 PluginDriver driver) {
        return Mono.empty();
    }

    /**
     * 当插件重新加载时触发
     *
     * @param driverId 驱动ID
     * @param driver   驱动
     * @return void
     */
    default Mono<Void> onReload(String driverId,
                                PluginDriver oldDriver,
                                PluginDriver driver) {
        return Mono.empty();
    }

    /**
     * 当插件卸载时触发
     *
     * @param driverId 驱动ID
     * @param driver   驱动
     * @return void
     */
    default Mono<Void> onUninstall(String driverId,
                                   PluginDriver driver) {
        return Mono.empty();
    }

    /**
     * 当插件管理器启动完成时触发
     *
     * @return void
     */
    default Mono<Void> onStartup() {
        return Mono.empty();
    }


}
