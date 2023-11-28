package org.jetlinks.community.rule.engine.event.handler;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.rule.engine.defaults.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Order(3)
public class RuleLogHandler {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Subscribe("/rule-engine/*/*/event/${rule.engine.event.level:error}")
    public Mono<Void> handleEvent(TopicPayload event) {
        return elasticSearchService.commit(RuleEngineLoggerIndexProvider.RULE_EVENT_LOG, RuleEngineExecuteEventInfo.of(event));
    }

    @Subscribe("/rule-engine/*/*/logger/${rule.engine.logging.level:info,warn,error}")
    public Mono<Void> handleLog(LogEvent event) {
        JSONObject jsonObject = FastBeanCopier.copy(event, new JSONObject());
        jsonObject.put("createTime", System.currentTimeMillis());
        return elasticSearchService.commit(RuleEngineLoggerIndexProvider.RULE_LOG, jsonObject);
    }
}
