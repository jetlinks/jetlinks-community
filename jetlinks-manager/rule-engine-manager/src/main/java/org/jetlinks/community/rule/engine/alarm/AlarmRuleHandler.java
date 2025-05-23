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

import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.alarm.AlarmConstants.ConfigKey;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 告警规则数据处理器,当场景规则中配置的告警动作被执行时,将调用此处理器的相关方法.
 *
 * @author zhouhao
 * @see AlarmTaskExecutorProvider
 * @since 2.0
 */
public interface AlarmRuleHandler {

    /**
     * 触发告警
     *
     * @param context 告警规则上下文
     * @param data    告警数据
     * @return 处理结果
     * @see org.jetlinks.community.rule.engine.enums.AlarmMode#trigger
     */
    Flux<Result> triggered(ExecutionContext context, RuleData data);

    /**
     * 解除告警
     *
     * @param context 告警规则上下文
     * @param data    告警数据
     * @return 处理结果
     * @see org.jetlinks.community.rule.engine.enums.AlarmMode#relieve
     */
    Flux<Result> relieved(ExecutionContext context, RuleData data);

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    class Result {

        @Schema(description = "告警配置ID")
        private String alarmConfigId;

        @Schema(description = "告警名称")
        private String alarmName;

        @Schema(description = "告警说明")
        private String description;

        @Schema(description = "是否重复告警")
        private boolean alarming;

        @Schema(description = "当前首次触发")
        private boolean firstAlarm;

        @Schema(description = "告警级别")
        private int level;

        @Schema(description = "上一次告警时间")
        private long lastAlarmTime;

        @Schema(description = "首次告警或者解除告警后的再一次告警时间.")
        private long alarmTime;

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

        @Schema(description = "告警来源名称")
        private String sourceName;

        /**
         * 标识告警触发的配置来自什么业务功能
         */
        @Schema(description = "告警配置源")
        private String alarmConfigSource = ConfigKey.alarmConfigSource;


        public Result copyWith(AlarmTargetInfo targetInfo) {
            Result result = FastBeanCopier.copy(this, new Result());
            result.setTargetType(targetInfo.getTargetType());
            result.setTargetId(targetInfo.getTargetId());
            result.setTargetName(targetInfo.getTargetName());
            result.setSourceType(targetInfo.getSourceType());
            result.setSourceId(targetInfo.getSourceId());
            result.setSourceName(targetInfo.getSourceName());
            return result;
        }


        public Map<String, Object> toMap() {
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(16);

            map.put(ConfigKey.alarmConfigId, alarmConfigId);
            map.put(ConfigKey.alarmName, alarmName);
            map.put(ConfigKey.alarming, alarming);
            map.put(ConfigKey.firstAlarm, firstAlarm);
            map.put(ConfigKey.level, level);
            map.put(ConfigKey.alarmTime, alarmTime);
            map.put(ConfigKey.lastAlarmTime, lastAlarmTime);

            map.put(ConfigKey.targetType, targetType);
            map.put(ConfigKey.targetId, targetId);
            map.put(ConfigKey.targetName, targetName);

            map.put(ConfigKey.sourceType, sourceType);
            map.put(ConfigKey.sourceId, sourceId);
            map.put(ConfigKey.sourceName, sourceName);

            return map;
        }
    }

}
