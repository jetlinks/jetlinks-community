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


import org.hswebframework.web.i18n.LocaleUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * @author bestfeng
 */
@Component
public class SceneAlarmTarget extends AbstractAlarmTarget {

    public static final String TYPE = "scene";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return LocaleUtils
            .resolveMessage("message.rule_engine_alarm_scene", "场景");
    }

    @Override
    public Integer getOrder() {
        return 400;
    }

    @Override
    public Flux<AlarmTargetInfo> doConvert(AlarmData data) {
        return Flux.just(AlarmTargetInfo
            .of(data.getRuleId(),
                data.getRuleName(),
                getType(),
                data.getCreatorId())
            .withSource(TYPE, data.getRuleId(), data.getRuleName()));
    }

}
