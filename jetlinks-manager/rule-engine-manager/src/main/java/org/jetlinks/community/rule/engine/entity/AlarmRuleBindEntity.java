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
package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.utils.DigestUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Table(name = "s_alarm_rule_bind", indexes = {
        @Index(name = "idx_alarm_rule_aid", columnList = "alarmId"),
        @Index(name = "idx_alarm_rule_rid", columnList = "ruleId"),
})
@Getter
@Setter
@Schema(description = "告警规则绑定信息")
@EnableEntityEvent
public class AlarmRuleBindEntity extends GenericEntity<String> {

    public static final int ANY_BRANCH_INDEX = -1;

    @Column(nullable = false, updatable = false, length = 64)
    @NotBlank
    @Schema(description = "告警ID")
    private String alarmId;

    @Column(nullable = false, updatable = false, length = 64)
    @NotBlank
    @Schema(description = "场景规则ID")
    private String ruleId;

    @Column(nullable = false, updatable = false)
    @Schema(description = "规则条件分支ID")
    @DefaultValue("-1")
    private Integer branchIndex;

    @Override
    public String getId() {
        if (StringUtils.hasText(super.getId())) {
            return super.getId();
        }
        setId(DigestUtils.md5Hex(String.join("|", alarmId, ruleId, String.valueOf(branchIndex))));
        return super.getId();
    }
}
