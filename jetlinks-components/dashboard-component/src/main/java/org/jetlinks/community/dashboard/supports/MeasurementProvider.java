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
package org.jetlinks.community.dashboard.supports;

import org.jetlinks.community.dashboard.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MeasurementProvider {

    /**
     * @return 仪表定义
     * @see DefaultDashboardDefinition
     */
    DashboardDefinition getDashboardDefinition();

    /**
     * @return 对象定义
     * @see org.jetlinks.community.dashboard.measurements.SystemObjectDefinition
     */
    ObjectDefinition getObjectDefinition();

    /**
     * @return 全部指标
     */
    Flux<Measurement> getMeasurements();

    /**
     * @param id 指标ID {@link Measurement#getDefinition()} {@link MeasurementDefinition#getId()}
     * @return 对应等指标, 不存在则返回 {@link Mono#empty()}
     * @see MeasurementDefinition
     */
    Mono<Measurement> getMeasurement(String id);

}
