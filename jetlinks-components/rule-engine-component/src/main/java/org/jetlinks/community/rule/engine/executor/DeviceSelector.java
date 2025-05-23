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
package org.jetlinks.community.rule.engine.executor;

import org.jetlinks.core.device.DeviceOperator;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 设备选择器,根据上下文来选择设备
 *
 * @author zhouhao
 * @since 1.5
 */
public interface DeviceSelector {

    /**
     * 根据上下文选择设备
     *
     * @param context 上下文数据
     * @return 设备列表
     */
    Flux<DeviceOperator> select(Map<String, Object> context);

}
