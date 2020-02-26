package org.jetlinks.community.rule.engine.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.ExecuteLogInfo;
import org.jetlinks.rule.engine.api.events.NodeExecuteEvent;
import org.jetlinks.rule.engine.api.events.RuleEvent;
import org.jetlinks.rule.engine.cluster.logger.LogInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Order(3)
public class RuleLogHandler {

    @Autowired
    private ElasticSearchService elasticSearchService;


    @EventListener
    public void handleRuleLog(LogInfo event) {
        ExecuteLogInfo logInfo = FastBeanCopier.copy(event, new ExecuteLogInfo());
        elasticSearchService.commit(RuleEngineLoggerIndexProvider.RULE_LOG, Mono.just(logInfo))
            .subscribe();
    }

    @EventListener
    public void handleRuleExecuteEvent(NodeExecuteEvent event) {
        //不记录BEFORE和RESULT事件
        if (!RuleEvent.NODE_EXECUTE_BEFORE.equals(event.getEvent())
            && !RuleEvent.NODE_EXECUTE_RESULT.equals(event.getEvent())) {
            RuleEngineExecuteEventInfo eventInfo = FastBeanCopier.copy(event, new RuleEngineExecuteEventInfo());
            elasticSearchService.commit(RuleEngineLoggerIndexProvider.RULE_LOG, Mono.just(eventInfo))
                .subscribe();
        }
    }

}
