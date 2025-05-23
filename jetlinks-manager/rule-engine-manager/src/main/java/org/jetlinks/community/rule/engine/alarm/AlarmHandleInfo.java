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
package org.jetlinks.community.rule.engine.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.enums.AlarmHandleState;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.jetlinks.community.terms.TermSpec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmHandleInfo {

    @Schema(description = "告警记录ID")
    @NotBlank
    private String alarmRecordId;

    @Schema(description = "告警ID")
    @NotBlank
    private String alarmConfigId;

    @Schema(description = "告警时间")
    @NotNull
    private Long alarmTime;

    @Schema(description = "处理说明")
    private String describe;

    @Schema(description = "处理时间")
    private Long handleTime;

    @Schema(description = "处理类型")
    private String type;

    @Schema(description = "处理后的状态")
    private AlarmRecordState state;

    @Schema(description = "告警处理状态")
    private AlarmHandleState handleState;

    @Schema(description = "告警记录创建者ID")
    private String recordCreatorId;

    @Schema(description = "告警级别")
    private int level;

    @Schema(description = "告警目标类型")
    private String targetType;

    @Schema(description = "告警目标名称")
    private String targetName;

    @Schema(description = "告警目标Id")
    private String targetId;

    @Schema(description = "告警源类型")
    private String sourceType;

    @Schema(description = "告警源Id")
    private String sourceId;

    @Schema(description = "告警源名称")
    private String sourceName;

    @Schema(description = "告警配置源")
    private String alarmConfigSource;

    @Schema(description = "触发条件")
    private TermSpec termSpec;

    @Schema(description = "触发条件描述")
    private String triggerDesc;

    @Schema(description = "告警原因描述")
    private String actualDesc;


}
