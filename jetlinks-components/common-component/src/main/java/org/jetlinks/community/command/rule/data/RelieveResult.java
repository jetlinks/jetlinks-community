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

/**
 * 解除警告结果
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelieveResult extends AlarmResult{

    @Schema(description = "告警级别")
    private int level;

    @Schema(description = "告警原因描述")
    private String actualDesc;

    @Schema(description = "解除原因")
    private String relieveReason;

    @Schema(description = "解除时间")
    private long relieveTime;

    @Schema(description = "解除说明")
    private String describe;
}