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

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.rule.engine.alarm.AlarmHandler;
import org.jetlinks.community.rule.engine.cmd.*;
import org.jetlinks.community.rule.engine.entity.AlarmLevelEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRuleBindEntity;
import org.jetlinks.community.rule.engine.scene.*;
import org.jetlinks.community.rule.engine.service.*;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
public class RuleEngineManagerConfiguration {

    @Bean
    public SceneTaskExecutorProvider sceneTaskExecutorProvider(EventBus eventBus,
                                                               ObjectProvider<SceneFilter> filters,
                                                               ObjectProvider<SceneActionProvider<?>> providers,
                                                               ObjectProvider<SceneTriggerProvider<?>> triggerProviders) {
        providers.forEach(SceneProviders::register);
        triggerProviders.forEach(SceneProviders::register);
        return new SceneTaskExecutorProvider(eventBus,
                                             SceneFilter.composite(filters));
    }

    @Bean
    @ConditionalOnClass(AlarmRuleBindEntity.class)
    public RuleCommandSupportManager alarmRuleBindCommandSupportManager(SceneService sceneService,
                                                                        AlarmConfigService configService,
                                                                        ReactiveRepository<AlarmLevelEntity, String> alarmLevelRepository,
                                                                        AlarmRecordService recordService,
                                                                        AlarmHandleHistoryService handleHistoryService,
                                                                        AlarmRuleBindService ruleBindService,
                                                                        AlarmHistoryService historyService,
                                                                        AlarmHandler alarmHandler) {
        return new RuleCommandSupportManager(
            new SceneCommandSupport(sceneService),
            new AlarmHistoryCommandSupport(historyService),
            new AlarmConfigCommandSupport(configService, alarmLevelRepository),
            new AlarmRecordCommandSupport(recordService, handleHistoryService),
            new AlarmRuleBindCommandSupport(ruleBindService),
            new AlarmCommandSupport(alarmHandler)
        );
    }

    @Bean(initMethod = "init")
    public TimeSeriesAlarmHistoryService alarmHistoryService(TimeSeriesManager timeSeriesManager) {
        return new TimeSeriesAlarmHistoryService(timeSeriesManager);
    }
}
