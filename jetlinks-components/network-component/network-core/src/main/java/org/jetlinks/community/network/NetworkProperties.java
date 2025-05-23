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
package org.jetlinks.community.network;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class NetworkProperties implements Serializable, ValueObject {
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    private String id;

    /**
     * 网络类型
     * @see NetworkType#getId()
     */
    private String type;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置是否启用
     */
    private boolean enabled;

    /**
     * 配置内容，不同的网络组件，配置内容不同
     */
    private Map<String, Object> configurations;

    @Override
    public Map<String, Object> values() {
        return configurations;
    }
}
