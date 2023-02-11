package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.AlarmRuleBindEntity;
import org.springframework.stereotype.Component;

/**
 * 告警规则绑定.
 *
 * @author zhangji 2022/11/23
 */
@Component
@AllArgsConstructor
public class AlarmRuleBindService extends GenericReactiveCrudService<AlarmRuleBindEntity, String> {

}
