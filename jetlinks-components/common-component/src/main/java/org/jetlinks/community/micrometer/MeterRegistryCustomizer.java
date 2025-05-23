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

/**
 * 监控指标自定义注册接口,用于对指标进行自定义,如添加指标标签等操作
 *
 * @author zhouhao
 * @since 1.11
 */
public interface MeterRegistryCustomizer {

    /**
     * 在指标首次初始化时调用,可以通过判断metric进行自定义标签
     *
     * @param metric   指标
     * @param settings 自定义设置
     */
    void custom(String metric, MeterRegistrySettings settings);

}
