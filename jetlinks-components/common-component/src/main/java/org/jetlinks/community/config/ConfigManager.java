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

import org.jetlinks.community.ValueObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 配置管理器,统一管理系统相关配置信息
 *
 * @author zhouhao
 * @since 2.0
 */
public interface ConfigManager {

    /**
     * 获取全部已经定义的配置作用域
     *
     * @return 配置作用域
     */
    Flux<ConfigScope> getScopes();

    /**
     * 获取根据作用域ID获取已经定义的配置作用域
     *
     * @return 配置作用域
     */
    Mono<ConfigScope> getScope(String scope);

    /**
     * 获取指定作用域下的属性定义信息
     *
     * @param scope 配置作用域
     * @return 属性定义信息
     */
    Flux<ConfigPropertyDef> getPropertyDef(String scope);

    /**
     * 获取作用于下的全部配置
     *
     * @param scope 配置作用域
     * @return 配置信息
     */
    Mono<ValueObject> getProperties(String scope);

    /**
     * 设置作用域下的配置
     *
     * @param scope  作用域
     * @param values 配置信息
     * @return void
     */
    Mono<Void> setProperties(String scope, Map<String, Object> values);

}
