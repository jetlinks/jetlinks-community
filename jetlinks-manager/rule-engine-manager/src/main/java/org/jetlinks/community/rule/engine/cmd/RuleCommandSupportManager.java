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
