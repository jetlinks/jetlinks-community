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

import org.jetlinks.community.command.CrudCommandSupport;
import org.jetlinks.community.rule.engine.entity.AlarmRuleBindEntity;
import org.jetlinks.community.rule.engine.service.AlarmRuleBindService;

/**
 * @author liusq
 * @date 2024/4/12
 */
public class AlarmRuleBindCommandSupport extends CrudCommandSupport<AlarmRuleBindEntity> {
    public AlarmRuleBindCommandSupport(AlarmRuleBindService ruleBindService) {
        super(ruleBindService);
    }
}
