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

import java.io.Serializable;

/**
 * 告警结果
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AlarmResult implements Serializable {

    private static final long serialVersionUID = -1752497262936740164L;

    @Schema(description = "告警ID")
    private String recordId;

    @Schema(description = "是否重复告警")
    private boolean alarming;

    @Schema(description = "当前首次触发")
    private boolean firstAlarm;

    @Schema(description = "上一次告警时间")
    private long lastAlarmTime;

    @Schema(description = "首次告警或者解除告警后的再一次告警时间")
    private long alarmTime;
}