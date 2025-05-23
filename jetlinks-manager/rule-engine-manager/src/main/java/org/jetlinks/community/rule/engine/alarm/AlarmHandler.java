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

import org.jetlinks.community.command.rule.data.AlarmInfo;
import org.jetlinks.community.command.rule.data.AlarmResult;
import org.jetlinks.community.command.rule.data.RelieveInfo;
import org.jetlinks.community.command.rule.data.RelieveResult;
import reactor.core.publisher.Mono;


/**
 * 告警处理支持
 */
public interface AlarmHandler {

    /**
     * 触发告警
     * @see AlarmTaskExecutorProvider
     * @see org.jetlinks.community.things.impl.preprocessor.internal.AbstractMessageAlarmPreprocessor
     *
     * @return 告警触发结果
     */
    Mono<AlarmResult> triggerAlarm(AlarmInfo alarmInfo);

    /**
     * 解除告警
     * @see AlarmTaskExecutorProvider
     *
     * @return 告警解除结果
     */
    Mono<RelieveResult> relieveAlarm(RelieveInfo relieveInfo);
}
