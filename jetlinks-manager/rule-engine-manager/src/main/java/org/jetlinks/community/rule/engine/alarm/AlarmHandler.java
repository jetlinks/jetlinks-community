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
