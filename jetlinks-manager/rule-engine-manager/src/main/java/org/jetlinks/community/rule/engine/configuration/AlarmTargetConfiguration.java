package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.community.rule.engine.alarm.AlarmRuleHandler;
import org.jetlinks.community.rule.engine.alarm.AlarmTarget;
import org.jetlinks.community.rule.engine.alarm.AlarmTaskExecutorProvider;
import org.jetlinks.community.rule.engine.alarm.CustomAlarmTargetSupplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 *
 * @author zhangji 2025/1/15
 * @since 2.3
 */
@AutoConfiguration
public class AlarmTargetConfiguration {

    @Bean
    public AlarmTaskExecutorProvider alarmTaskExecutorProvider(AlarmRuleHandler alarmHandler,
                                                               ObjectProvider<AlarmTarget> targetProviders) {
        targetProviders.forEach(CustomAlarmTargetSupplier::register);
        return new AlarmTaskExecutorProvider(alarmHandler);
    }

}
