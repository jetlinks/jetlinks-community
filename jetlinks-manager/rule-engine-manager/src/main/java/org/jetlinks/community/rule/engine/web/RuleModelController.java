package org.jetlinks.community.rule.engine.web;

import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.rule.engine.entity.RuleModelEntity;
import org.jetlinks.community.rule.engine.service.RuleModelService;
import org.jetlinks.rule.engine.api.executor.ExecutableRuleNodeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("rule-engine/model")
@Resource(id = "rule-model", name = "规则引擎-模型")
public class RuleModelController implements ReactiveServiceCrudController<RuleModelEntity, String> {

    @Autowired
    private RuleModelService ruleModelService;

    @Autowired
    private ExecutableRuleNodeFactory factory;

    @Override
    public ReactiveCrudService<RuleModelEntity, String> getService() {
        return ruleModelService;
    }

    @PostMapping("/{id}/_deploy")
    @ResourceAction(id = "deploy", name = "发布")
    public Mono<Boolean> deploy(@PathVariable String id) {
        return ruleModelService.deploy(id);
    }

    //获取全部支持的执行器
    @GetMapping("/executors")
    @QueryAction
    public Flux<String> getAllSupportExecutors() {
        return Flux.fromIterable(factory.getAllSupportExecutor());
    }
}
