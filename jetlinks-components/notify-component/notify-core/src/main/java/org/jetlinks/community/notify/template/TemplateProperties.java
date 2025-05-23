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
package org.jetlinks.community.notify.template;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.DefaultNotifyType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TemplateProperties implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 模版ID
     */
    private String id;

    /**
     * 模版名称
     */
    private String name;

    /**
     * 固定配置ID
     */
    private String configId;

    /**
     * 类型
     *
     * @see DefaultNotifyType
     */
    private String type;

    /**
     * 服务商
     *
     * @see TemplateProvider#getProvider()
     */
    private String provider;

    /**
     * 模版内容,不同服务商格式不同
     */
    private Map<String,Object> template;

    /**
     * 变量定义,用于声明此模版所需要的变量信息,方便在使用模版时能知道应该传入的参数.
     */
    private List<VariableDefinition> variableDefinitions;

    /**
     * 模版说明
     */
    private String description;

}
