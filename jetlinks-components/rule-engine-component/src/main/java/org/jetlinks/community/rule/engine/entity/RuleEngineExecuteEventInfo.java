/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
