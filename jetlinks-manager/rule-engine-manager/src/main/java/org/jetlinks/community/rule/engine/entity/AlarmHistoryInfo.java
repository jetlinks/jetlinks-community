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

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.alarm.AlarmTargetInfo;
import org.jetlinks.community.rule.engine.scene.SceneData;
import org.jetlinks.community.terms.TermSpec;

import java.io.Serializable;


@Getter
@Setter
public class AlarmHistoryInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "告警数据ID")
    private String id;

    @Schema(description = "告警配置ID")
    private String alarmConfigId;

    @Schema(description = "告警配置名称")
    private String alarmConfigName;

    @Schema(description = "告警记录ID")
    private String alarmRecordId;

    @Schema(description = "告警级别")
    private int level;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "告警时间")
    private long alarmTime;

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

    @Schema(description = "告警信息")
    private String alarmInfo;

    @Schema(description = "创建者ID")
    private String creatorId;

    @Schema(description = "触发条件")
    private TermSpec termSpec;

    @Schema(description = "告警配置源")
    private String alarmConfigSource;

    @Schema(description = "触发条件描述")
    private String triggerDesc;

    @Schema(description = "告警原因描述")
    private String actualDesc;

    public void withTermSpec(TermSpec termSpec){
        if (termSpec != null) {
            this.setTermSpec(termSpec);
            this.setTriggerDesc(termSpec.getTriggerDesc());
            this.setActualDesc(termSpec.getActualDesc());
        }
    }

    @Deprecated
    public static AlarmHistoryInfo of(String alarmRecordId,
                                      AlarmTargetInfo targetInfo,
                                      SceneData data,
                                      AlarmConfigEntity alarmConfig) {
        AlarmHistoryInfo info = new AlarmHistoryInfo();
        info.setAlarmConfigId(alarmConfig.getId());
        info.setAlarmConfigName(alarmConfig.getName());
        info.setAlarmRecordId(alarmRecordId);
        info.setLevel(alarmConfig.getLevel());
        info.setId(data.getId());
        info.setAlarmTime(System.currentTimeMillis());

        info.setTargetName(targetInfo.getTargetName());
        info.setTargetId(targetInfo.getTargetId());
        info.setTargetType(targetInfo.getTargetType());

        info.setSourceName(targetInfo.getSourceName());
        info.setSourceType(targetInfo.getSourceType());
        info.setSourceId(targetInfo.getSourceId());

        info.setAlarmInfo(JSON.toJSONString(data.getOutput()));
        info.setDescription(alarmConfig.getDescription());
        return info;
    }
}
