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
package org.jetlinks.community.micrometer;

import org.jetlinks.core.metadata.DataType;

import javax.annotation.Nonnull;

/**
 * 指标注册配置信息
 *
 * @author zhouhao
 * @since 1.11
 */
public interface MeterRegistrySettings {

    /**
     * 给指标添加标签,用于自定义标签类型.在相应指标实现中会根据类型对数据进行存储
     *
     * @param tag  标签key
     * @param type 类型
     */
    void addTag(@Nonnull String tag, @Nonnull DataType type);

}
