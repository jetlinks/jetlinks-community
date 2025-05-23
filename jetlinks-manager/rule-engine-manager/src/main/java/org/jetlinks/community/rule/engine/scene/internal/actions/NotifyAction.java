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
package org.jetlinks.community.rule.engine.scene.internal.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.relation.utils.VariableSource;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jetlinks.community.rule.engine.scene.SceneAction.parseColumnFromOptions;

@Getter
@Setter
public class NotifyAction implements Serializable {
    @Schema(description = "通知类型")
    @NotBlank(message = "error.scene_rule_actions_notify_type_cannot_be_empty")
    private String notifyType;

    @Schema(description = "通知配置ID")
    @NotBlank(message = "error.scene_rule_actions_notify_id_cannot_be_empty")
    private String notifierId;

    @Schema(description = "通知模版ID")
    @NotBlank(message = "error.scene_rule_actions_notify_template_cannot_be_blank")
    private String templateId;

    /**
     * 变量值的格式可以为{@link  VariableSource}
     */
    @Schema(description = "通知变量")
    @NotBlank(message = "error.scene_rule_actions_notify_variables_cannot_be_blank")
    private Map<String, Object> variables;

    public List<String> parseColumns() {
        if (MapUtils.isEmpty(variables)) {
            return Collections.emptyList();
        }
        return variables
            .values()
            .stream()
            .flatMap(val -> parseColumnFromOptions(VariableSource.of(val).getOptions()).stream())
            .collect(Collectors.toList());
    }
}
