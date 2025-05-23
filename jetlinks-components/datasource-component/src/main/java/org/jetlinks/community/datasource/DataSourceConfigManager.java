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
package org.jetlinks.community.datasource;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * 数据源配置管理器,统一管理数据源配置
 *
 * @author zhouhao
 * @since 1.10
 */
public interface DataSourceConfigManager {

    /**
     * 根据类型ID和数据源ID获取配置
     *
     * @param typeId       类型ID
     * @param datasourceId 数据源ID
     * @return 配置信息
     */
    Mono<DataSourceConfig> getConfig(String typeId, String datasourceId);

    /**
     * 监听配置变化，当有配置变化后将调用回调参数
     *
     * @param callback 回调参数
     */
    Disposable doOnConfigChanged(BiFunction<ConfigState, DataSourceConfig,Mono<Void>> callback);

    enum ConfigState{
        normal,
        disabled
    }
}
