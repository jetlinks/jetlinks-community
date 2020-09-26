package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.rule.engine.entity.RuleModelEntity;
import org.jetlinks.community.rule.engine.service.RuleModelService;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.worker.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@RestController
@RequestMapping("rule-engine/model")
@Resource(id = "rule-model", name = "规则引擎-模型")
@Deprecated
@Hidden
public class RuleModelController implements ReactiveServiceCrudController<RuleModelEntity, String> {

    @Autowired
    private RuleModelService ruleModelService;

    @Autowired
    private RuleEngine ruleEngine;

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
        return ruleEngine.getWorkers().flatMap(Worker::getSupportExecutors).flatMapIterable(Function.identity());
    }
}
