package org.jetlinks.community.rule.engine.web;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteLogInfo;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.service.RuleInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("rule-engine/instance")
@Resource(id = "rule-instance", name = "规则引擎-实例")
public class RuleInstanceController implements ReactiveServiceCrudController<RuleInstanceEntity, String> {

    @Autowired
    private RuleInstanceService instanceService;


    @PostMapping("/{id}/_start")
    @ResourceAction(id = "start", name = "启动")
    public Mono<Boolean> start(@PathVariable String id) {
        return instanceService.start(id)
            .thenReturn(true);
    }

    @PostMapping("/{id}/_stop")
    @ResourceAction(id = "stop", name = "停止")
    public Mono<Boolean> stop(@PathVariable String id) {
        return instanceService.stop(id)
            .thenReturn(true);
    }

    @GetMapping("/{id}/logs")
    @QueryAction
    public Mono<PagerResult<RuleEngineExecuteLogInfo>> queryLog(@PathVariable String id,
                                                                QueryParamEntity paramEntity) {
        return paramEntity.toQuery()
            .is("instanceId", id)
            .execute(instanceService::queryExecuteLog);
    }

    @GetMapping("/{id}/events")
    @QueryAction
    public Mono<PagerResult<RuleEngineExecuteEventInfo>> queryEvents(@PathVariable String id,
                                                                     QueryParamEntity paramEntity) {
        return paramEntity.toQuery()
            .is("instanceId", id)
            .execute(instanceService::queryExecuteEvent);

    }


    @Override
    public ReactiveCrudService<RuleInstanceEntity, String> getService() {
        return instanceService;
    }
}
