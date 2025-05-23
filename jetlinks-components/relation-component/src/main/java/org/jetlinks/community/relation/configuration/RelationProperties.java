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
package org.jetlinks.community.relation.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关系配置
 *
 * @author zhouhao
 * @since 2.0
 */
@ConfigurationProperties(prefix = "relation")
@Getter
@Setter
public class RelationProperties {

    /**
     * <pre>{@code
     * relation:
     *     relatable:
     *          device: user,org
     * }</pre>
     * 可建立关系的对象类型映射
     */
    private Map<String, List<String>> relatable = new HashMap<>();


    public List<String> getRelatable(String type) {
        return relatable.getOrDefault(type, Collections.emptyList());
    }
}
