package org.jetlinks.community.command.rule;

public interface RuleCommandServices {
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

    /**
     * 告警相关
     */
    String alarm = "alarm";

}