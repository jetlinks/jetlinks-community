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
package org.jetlinks.community.notify;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * 通知配置属性
 *
 * @author zhouhao
 * @see NotifyConfigManager
 * @since 1.0
 */
@Getter
@Setter
public class NotifierProperties implements ValueObject, Serializable {

    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 配置全局唯一标识
     */
    private String id;

    /**
     * 通知类型标识
     *
     * @see NotifyType
     */
    private String type;

    /**
     * 通知服务提供商标识,如: aliyun ...
     */
    private String provider;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置内容,不同的服务提供商,配置不同.
     *
     * @see NotifierProvider
     */
    private Map<String, Object> configuration;

    @Deprecated
    public Optional<Object> getConfig(String key) {
        return Optional.ofNullable(configuration)
            .map(conf -> conf.get(key));
    }

    @Deprecated
    public Object getConfigOrNull(String key) {
        return Optional.ofNullable(configuration)
            .map(conf -> conf.get(key))
            .orElse(null);
    }

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
