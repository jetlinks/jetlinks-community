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

import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;

/**
 * 设备选择器构造器,根据表达式来构造一个选择器.
 *
 * @author zhouhao
 * @see DeviceSelector
 * @since 1.5
 */
public interface DeviceSelectorBuilder {


    /**
     * 根据选择器描述来创建选择器
     *
     * @param spec 描述
     * @return 选择器
     * @see org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider
     */
    DeviceSelector createSelector(DeviceSelectorSpec spec);
}
