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

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.alarm.AlarmConstants;
import org.jetlinks.community.rule.engine.alarm.AlarmTaskExecutorProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;

import java.util.ArrayList;
import java.util.List;

import static org.jetlinks.community.rule.engine.scene.Variable.OPTION_PARAMETER;

@Getter
@Setter
public class AlarmAction extends AlarmTaskExecutorProvider.Config {

    public static final String OPTION_ALARM_ID = "id";
    public static final String OPTION_ALARM_NAME = "name";
    public static final String OPTION_ALARM_LEVEL = "level";
    /**
     * @see org.jetlinks.community.rule.engine.alarm.AlarmRuleHandler.Result
     */
    public List<Variable> createVariables() {

        List<Variable> variables = new ArrayList<>();

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.alarmConfigId,
                        LocaleUtils.resolveMessage("message.alarm_config_id", "告警配置ID"))
                    .withType(StringType.GLOBAL)
                //用于前端
                    .withOption(OPTION_PARAMETER,OPTION_ALARM_ID)
        );

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.alarmName,
                        LocaleUtils.resolveMessage("message.alarm_config_name", "告警配置名称"))
                    .withType(StringType.GLOBAL)
                    .withOption(OPTION_PARAMETER,OPTION_ALARM_NAME)
        );

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.level,
                        LocaleUtils.resolveMessage("message.alarm_level", "告警级别"))
                    .withType(IntType.GLOBAL)
                    .withOption(OPTION_PARAMETER,OPTION_ALARM_LEVEL)
        );

//            variables.add(
//                Variable.of(AlarmConstants.ConfigKey.alarming,
//                            LocaleUtils.resolveMessage("message.is_alarming", "是否重复告警"))
//                        .withDescription(LocaleUtils.resolveMessage("message.is_alarming_description", "是否已存在告警中的记录"))
//                        .withType(BooleanType.GLOBAL)
//            );

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.firstAlarm,
                        LocaleUtils.resolveMessage("message.first_alarm", "是否首次告警"))
                    .withDescription(LocaleUtils.resolveMessage("message.first_alarm_description", "是否为首次告警或者解除后的第一次告警"))
                    .withType(BooleanType.GLOBAL)
                    .withOption("bool", BooleanType.GLOBAL)
        );

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.alarmTime,
                        LocaleUtils.resolveMessage("message.alarm_time", "首次告警时间"))
                    .withDescription(LocaleUtils.resolveMessage("message.alarm_time_description", "首次告警或者解除告警后的第一次告警时间"))
                    .withType(DateTimeType.GLOBAL)
        );

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.lastAlarmTime,
                        LocaleUtils.resolveMessage("message.last_alarm_time", "上一次告警时间"))
                    .withDescription(LocaleUtils.resolveMessage("message.last_alarm_time_description", "上一次触发告警的时间"))
                    .withType(DateTimeType.GLOBAL)
        );


        return variables;
    }
}
