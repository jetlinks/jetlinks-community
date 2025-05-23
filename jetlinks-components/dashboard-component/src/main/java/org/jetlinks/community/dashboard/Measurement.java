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
package org.jetlinks.community.dashboard;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 度量,指标. 如: 使用率
 *
 * @author zhouhao
 * @since 1.0
 */
public interface Measurement {

    MeasurementDefinition getDefinition();

    /**
     * 获取所有指标维度
     *
     * @return 维度
     */
    Flux<MeasurementDimension> getDimensions();

    /**
     * 获取指定ID的维度
     *
     * @param id 维度定义ID
     * @return 指定的维度, 不存在则返回 {@link Mono#empty()}
     */
    Mono<MeasurementDimension> getDimension(String id);

}
