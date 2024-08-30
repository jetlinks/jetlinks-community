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
import java.util.function.Function;

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

    @PostMapping("/{alarmId}/{ruleId}/_delete")
    @DeleteAction
    @Operation(summary = "删除指定分支的告警规则绑定")
    public Mono<Integer> deleteAlarmBindByBranchId(@PathVariable @Parameter(description = "告警配置ID") String alarmId,
                                                   @PathVariable @Parameter(description = "场景联动ID") String ruleId,
                                                   @RequestBody @Parameter(description = "分支ID") Mono<List<Integer>> branchIndex) {
        return branchIndex
            .flatMap(idList -> service
                .createDelete()
                .where(AlarmRuleBindEntity::getAlarmId, alarmId)
                .and(AlarmRuleBindEntity::getRuleId, ruleId)
                .in(AlarmRuleBindEntity::getBranchIndex, idList)
                .execute());
    }

    @PostMapping("/_delete")
    @DeleteAction
    @Operation(summary = "批量删除多个规则或告警的绑定")
    public Mono<Integer> deleteAlarmBindById(@RequestBody @Parameter(description = "绑定信息") Mono<List<AlarmRuleBindEntity>> payload) {
        return payload
            .flatMapIterable(Function.identity())
            .map(AlarmRuleBindEntity::getId)
            .as(service::deleteById);
    }
}
