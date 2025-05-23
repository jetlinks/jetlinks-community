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
package org.jetlinks.community.datasource.command;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.command.Command;
import org.jetlinks.community.datasource.DataSource;

import java.util.Map;

@Getter
@Setter
public class DataSourceCommandConfig {

    /**
     * 数据源类型
     *
     * @see DataSource#getType()
     */
    private String datasourceType;
    /**
     * 数据源ID
     *
     * @see DataSource#getId()
     */
    private String datasourceId;

    /**
     * 命令支持ID,比如一个数据源命令分类.
     *
     * @see org.jetlinks.community.command.CommandSupportManagerProvider#getCommandSupport(String, Map)
     */
    private String supportId;

    /**
     * 命令ID
     *
     * @see Command#getCommandId()
     */
    private String commandId;

    private String commandName;

    /**
     * 命令配置信息
     */
    private Map<String, Object> configuration;

}
