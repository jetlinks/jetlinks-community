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
package org.jetlinks.community.timeseries;

import reactor.core.publisher.Mono;

/**
 * 时序数据服务管理器,统一管理时序数据操作接口
 *
 * @author zhouhao
 * @since 1.0
 */
public interface TimeSeriesManager {

    /**
     * 根据指标获取服务
     *
     * @param metric 指标,通常表名
     * @return 时序服务
     */
    TimeSeriesService getService(TimeSeriesMetric metric);

    /**
     * 获取多个指标服务
     * @param metric 多个指标
     * @return 时序服务
     */
    TimeSeriesService getServices(TimeSeriesMetric... metric);

    TimeSeriesService getServices(String... metric);

    TimeSeriesService getService(String metric);

    /**
     * 注册元数据,将更新表结构
     *
     * @param metadata 元数据
     * @return 注册结果
     */
    Mono<Void> registerMetadata(TimeSeriesMetadata metadata);

}
