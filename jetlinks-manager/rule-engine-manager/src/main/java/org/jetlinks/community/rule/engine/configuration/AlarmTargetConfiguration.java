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
