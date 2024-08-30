package org.jetlinks.community.rule.engine.scene.internal.actions;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.rule.engine.alarm.AlarmConstants;
import org.jetlinks.community.rule.engine.alarm.AlarmTaskExecutorProvider;
import org.jetlinks.community.rule.engine.scene.Variable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AlarmAction extends AlarmTaskExecutorProvider.Config {

    public List<Variable> createVariables() {

        List<Variable> variables = new ArrayList<>();

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.alarmConfigId,
                        LocaleUtils.resolveMessage("message.alarm_config_id", "告警配置ID"))
                    .withType(StringType.GLOBAL)
        );

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.alarmName,
                        LocaleUtils.resolveMessage("message.alarm_config_name", "告警配置名称"))
                    .withType(StringType.GLOBAL)
        );

        variables.add(
            Variable.of(AlarmConstants.ConfigKey.level,
                        LocaleUtils.resolveMessage("message.alarm_level", "告警级别"))
                    .withType(IntType.GLOBAL)
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
