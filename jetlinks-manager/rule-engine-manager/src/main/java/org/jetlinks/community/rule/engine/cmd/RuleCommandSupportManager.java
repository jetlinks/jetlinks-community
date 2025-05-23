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
package org.jetlinks.community.rule.engine.cmd;

import org.jetlinks.community.command.InternalSdkServices;
import org.jetlinks.community.command.StaticCommandSupportManagerProvider;
import org.jetlinks.community.command.rule.RuleCommandServices;

/**
 * @author liusq
 * @date 2024/4/11
 */
public class RuleCommandSupportManager extends StaticCommandSupportManagerProvider {

    public RuleCommandSupportManager(SceneCommandSupport sceneCommandSupport,
                                     AlarmHistoryCommandSupport alarmHistoryCommandSupport,
                                     AlarmConfigCommandSupport alarmConfigCommandSupport,
                                     AlarmRecordCommandSupport alarmRecordCommandSupport,
                                     AlarmRuleBindCommandSupport alarmRuleBindCommandSupport,
                                     AlarmCommandSupport alarmCommandSupport) {
        super(InternalSdkServices.ruleService);
        register(RuleCommandServices.sceneService, sceneCommandSupport);
        register(RuleCommandServices.alarmHistoryService, alarmHistoryCommandSupport);
        register(RuleCommandServices.alarmConfigService, alarmConfigCommandSupport);
        register(RuleCommandServices.alarmRecordService, alarmRecordCommandSupport);
        register(RuleCommandServices.alarmRuleBindService, alarmRuleBindCommandSupport);
        register(RuleCommandServices.alarm, alarmCommandSupport);
    }
}
