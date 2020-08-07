package org.jetlinks.community.rule.engine.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.event.TopicPayload;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class RuleEngineExecuteEventInfo {
    private String id;

    private String event;

    private long createTime = System.currentTimeMillis();

    private String instanceId;

    private String nodeId;

    private String ruleData;

    private String contextId;

    public static RuleEngineExecuteEventInfo of(TopicPayload message) {
        Map<String, String> vars = message.getTopicVars("/rule-engine/{instanceId}/{nodeId}/event/{event}");
        RuleEngineExecuteEventInfo info = FastBeanCopier.copy(vars, new RuleEngineExecuteEventInfo());
        JSONObject json = message.bodyToJson();
        info.id=json.getString("id");
        info.contextId=json.getString("contextId");
        info.setRuleData(json.toJSONString());
        return info;
    }
}
