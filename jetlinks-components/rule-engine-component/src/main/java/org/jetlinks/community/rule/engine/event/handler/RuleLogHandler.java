package org.jetlinks.community.rule.engine.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteLogInfo;
import org.jetlinks.rule.engine.api.events.NodeExecuteEvent;
import org.jetlinks.rule.engine.api.events.RuleEvent;
import org.jetlinks.rule.engine.cluster.logger.LogInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(3)
public class RuleLogHandler {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Autowired
    private MessageGateway messageGateway;

    @EventListener
    public void handleRuleLog(LogInfo event) {
        RuleEngineExecuteLogInfo logInfo = FastBeanCopier.copy(event, new RuleEngineExecuteLogInfo());
        elasticSearchService.commit(RuleEngineLoggerIndexProvider.RULE_LOG, logInfo)
            .subscribe();
        // /rule-engine/{instanceId}/{nodeId}/log
        messageGateway
            .publish(String.join("/",
                "/rule-engine",
                event.getInstanceId(),
                event.getNodeId(),
                "log"), logInfo, true)
            .subscribe();
    }

    @EventListener
    public void handleRuleExecuteEvent(NodeExecuteEvent event) {
        //不记录BEFORE和RESULT事件
        if (!RuleEvent.NODE_EXECUTE_BEFORE.equals(event.getEvent())
            && !RuleEvent.NODE_EXECUTE_RESULT.equals(event.getEvent())) {
            RuleEngineExecuteEventInfo eventInfo = FastBeanCopier.copy(event, new RuleEngineExecuteEventInfo());
            elasticSearchService.commit(RuleEngineLoggerIndexProvider.RULE_EVENT_LOG, eventInfo)
                .subscribe();
        }
        // /rule-engine/{instanceId}/{nodeId}/event/{eventType}
        messageGateway
            .publish(String.join("/",
                "/rule-engine",
                event.getInstanceId(),
                event.getNodeId(),
                "event",
                event.getEvent().toLowerCase()
            ), event, true)
            .subscribe();
    }

}
