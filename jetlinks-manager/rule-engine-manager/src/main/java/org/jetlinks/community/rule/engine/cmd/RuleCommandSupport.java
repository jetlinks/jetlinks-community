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

/**
 * @author liusq
 * @date 2024/4/17
 */
@Deprecated
public interface RuleCommandSupport {
    /**
     * 场景
     */
    String sceneService = "sceneService";

    /**
     * 告警配置
     */
    String alarmConfigService = "alarmConfigService";

    /**
     * 告警记录
     */
    String alarmRecordService = "alarmRecordService";

    /**
     * 告警历史
     */
    String alarmHistoryService = "alarmHistoryService";

    /**
     * 告警规则绑定
     */
    String alarmRuleBindService = "alarmRuleBindService";
}
