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
package org.jetlinks.community.command.rule.data;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.terms.TermSpec;

import java.io.Serializable;
import java.util.Map;

/**
 * 触发告警参数
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlarmInfo implements Serializable {
    private static final long serialVersionUID = -2316376361116648370L;

    @Schema(description = "告警配置ID")
    private String alarmConfigId;

    @Schema(description = "告警名称")
    private String alarmName;

    @Schema(description = "告警说明")
    private String description;

    @Schema(description = "告警级别")
    private int level;

    @Schema(description = "告警目标类型")
    private String targetType;

    @Schema(description = "告警目标ID")
    private String targetId;

    @Schema(description = "告警目标名称")
    private String targetName;

    @Schema(description = "告警来源类型")
    private String sourceType;

    @Schema(description = "告警来源ID")
    private String sourceId;

    @Schema(description = "告警来源的创建人ID")
    private String sourceCreatorId;

    @Schema(description = "告警来源名称")
    private String sourceName;

    /**
     * 标识告警触发的配置来自什么业务功能
     */
    @Schema(description = "告警配置源")
    private String alarmConfigSource;

    @Schema(description = "告警数据")
    private Map<String, Object> data;

    /**
     * 告警触发条件
     */
    private TermSpec termSpec;

    @Schema(description = "告警时间")
    private Long alarmTime;

}