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
