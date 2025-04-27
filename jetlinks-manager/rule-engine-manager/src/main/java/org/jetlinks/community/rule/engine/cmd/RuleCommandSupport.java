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
