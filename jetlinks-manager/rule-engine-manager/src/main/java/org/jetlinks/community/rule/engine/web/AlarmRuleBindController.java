package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.rule.engine.entity.AlarmRuleBindEntity;
import org.jetlinks.community.rule.engine.service.AlarmRuleBindService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 告警规则绑定.
 *
 * @author zhangji 2022/11/23
 */
@RestController
@RequestMapping("/alarm/rule/bind")
@Resource(id = "alarm-config", name = "告警配置")
@Authorize
@Tag(name = "告警规则绑定")
@AllArgsConstructor
public class AlarmRuleBindController implements ReactiveServiceCrudController<AlarmRuleBindEntity, String> {

    private final AlarmRuleBindService service;

    @Override
    public ReactiveCrudService<AlarmRuleBindEntity, String> getService() {
        return service;
    }

    @PostMapping("/{alarmId}/_delete")
    @DeleteAction
    @Operation(summary = "批量删除告警规则绑定")
    public Mono<Integer> deleteAlarmBind(@PathVariable @Parameter(description = "告警配置ID") String alarmId,
                                         @RequestBody @Parameter(description = "场景联动ID") Mono<List<String>> ruleId) {
        return ruleId
            .flatMap(idList -> service
                .createDelete()
                .where(AlarmRuleBindEntity::getAlarmId, alarmId)
                .in(AlarmRuleBindEntity::getRuleId, idList)
                .execute());
    }

}
