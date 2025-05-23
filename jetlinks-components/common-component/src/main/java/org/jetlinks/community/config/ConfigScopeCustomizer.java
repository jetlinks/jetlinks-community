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
package org.jetlinks.community.config;

/**
 * 实现此接口,自定义配置域以及配置定义
 *
 * @author zhouhao
 * @since 2.0
 */
public interface ConfigScopeCustomizer {

    /**
     * 执行自定义,通过manager来添加自定义作用域
     *
     * @param manager manager
     */
    void custom(ConfigScopeManager manager);

}
