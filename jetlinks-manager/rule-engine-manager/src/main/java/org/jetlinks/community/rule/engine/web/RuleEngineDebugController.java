package org.jetlinks.community.rule.engine.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.rule.engine.service.DebugMessage;
import org.jetlinks.community.rule.engine.service.RuleEngineDebugService;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.RuleNodeConfiguration;
import org.jetlinks.rule.engine.api.model.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/rule-engine/debug")
@Authorize
@Resource(id="rule-model",name = "规则引擎-模型")
public class RuleEngineDebugController {

    @Autowired
    private RuleEngineDebugService ruleDebugService;

    @PostMapping
    public Mono<String> startSession() {
        return ruleDebugService.startSession();
    }

    @PostMapping("/{sessionId}")
    public Mono<String> startNode(@PathVariable String sessionId, @RequestBody RuleNodeConfiguration configuration) {
        return Mono.just(ruleDebugService.startNode(sessionId, configuration));
    }

    @GetMapping(value = "/{sessionId}/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DebugMessage> getSessionLogs(@PathVariable String sessionId) {
        return ruleDebugService.getDebugMessages(sessionId);
    }

    @PostMapping("/{sessionId}/{contextId}")
    public Mono<String> executeNode(@PathVariable String sessionId,
                                    @PathVariable String contextId,
                                    @RequestBody String data) {
        Object object = data.trim();
        if (data.startsWith("[") || data.startsWith("{")) {
            object = JSON.parse(data);
        }

        RuleData ruleData = RuleData.create(object);

        ruleDebugService.sendData(sessionId, contextId, ruleData);

        return Mono.just(ruleData.getId());
    }

    @GetMapping("/{sessionId}/contexts")
    public Flux<String> getSessionContexts(@PathVariable String sessionId) {
        return Flux.fromIterable(ruleDebugService.getAllContext(sessionId));
    }


    @PostMapping("/{sessionId}/condition")
    public Mono<Boolean> testCondition(@PathVariable String sessionId, @RequestBody JSONObject data) {
        return ruleDebugService.testCondition(sessionId, data.getJSONObject("condition").toJavaObject(Condition.class), data.get("data"));
    }

    @DeleteMapping("/{sessionId}")
    public Mono<Boolean> stopSession(@PathVariable String sessionId) {
        return   ruleDebugService.closeSession(sessionId);
    }

    @DeleteMapping("/{sessionId}/{contextId}")
    public Mono<Boolean> stopContext(@PathVariable String sessionId, @PathVariable String contextId) {

        return  ruleDebugService.stopContext(sessionId, contextId);
    }

}
