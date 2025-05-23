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
package org.jetlinks.community.rule.engine.web.response;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.alarm.AlarmTarget;

import java.util.List;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmTargetTypeInfo {

    private String id;

    private String name;

    private List<String> supportTriggers;

    public static AlarmTargetTypeInfo of(AlarmTarget type) {

        AlarmTargetTypeInfo info = new AlarmTargetTypeInfo();

        info.setId(type.getType());
        info.setName(type.getName());

        return info;

    }

    public AlarmTargetTypeInfo with(List<String> supportTriggers) {
        this.supportTriggers = supportTriggers;
        return this;
    }

    public String getName() {
        return LocaleUtils.resolveMessage("message.rule_engine_alarm_" + id, name);
    }
}
