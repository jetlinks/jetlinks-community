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
package org.jetlinks.community.rule.engine.log;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteLogInfo;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.defaults.LogEvent;
import org.springframework.beans.factory.SmartInitializingSingleton;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.jetlinks.core.metadata.SimplePropertyMetadata.of;

public class TimeSeriesRuleEngineLogService implements RuleEngineLogService, SmartInitializingSingleton {

    public static final TimeSeriesMetric RULE_LOG = TimeSeriesMetric.of("rule-engine-execute-log");
    public static final TimeSeriesMetric RULE_EVENT_LOG = TimeSeriesMetric.of("rule-engine-execute-event");

    private final TimeSeriesManager timeSeriesManager;

    public TimeSeriesRuleEngineLogService(TimeSeriesManager timeSeriesManager) {
        this.timeSeriesManager = timeSeriesManager;
    }


    /**
     * @see org.jetlinks.rule.engine.api.RuleConstants.Event
     */
    @Subscribe("/rule-engine/*/*/event/${rule.engine.event.level:error}")
    public Mono<Void> handleEvent(TopicPayload event) {

        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>(
                event.getTopicVars("/rule-engine/{instanceId}/{nodeId}/event/{event}")
        );

        RuleData ruleData = event.decode(RuleData.class);
        data.put("id", ruleData.getId());
        data.put("contextId", ruleData.getContextId());
        data.put("ruleData", ObjectMappers.toJsonString(ruleData));
        data.put("createTime", now);
        return timeSeriesManager
                .getService(RULE_EVENT_LOG)
                .commit(TimeSeriesData.of(now, data));
    }

    @Subscribe("/rule-engine/*/*/logger/${rule.engine.logging.level:info,warn,error}")
    public Mono<Void> handleLog(LogEvent event) {
        Map<String, Object> data = FastBeanCopier.copy(event, new HashMap<>());
        long now = System.currentTimeMillis();
        data.put("createTime", now);
        return timeSeriesManager
                .getService(RULE_LOG)
                .commit(TimeSeriesData.of(event.getTimestamp(), data));
    }

    public Mono<PagerResult<RuleEngineExecuteEventInfo>> queryEvent(QueryParam queryParam) {
        return timeSeriesManager
                .getService(RULE_EVENT_LOG)
                .queryPager(queryParam, ts -> ts.as(RuleEngineExecuteEventInfo.class));
    }

    public Mono<PagerResult<RuleEngineExecuteLogInfo>> queryLog(QueryParam queryParam) {
        return timeSeriesManager
                .getService(RULE_LOG)
                .queryPager(queryParam, ts -> ts.as(RuleEngineExecuteLogInfo.class));
    }

    @Override
    public void afterSingletonsInstantiated() {
        timeSeriesManager
                .registerMetadata(
                        TimeSeriesMetadata
                                .of(RULE_LOG,
                                    of("createTime", "创建时间", DateTimeType.GLOBAL),
                                    of("timestamp", "日志时间", DateTimeType.GLOBAL),
                                    of("level", "日志级别", StringType.GLOBAL),
                                    of("message", "消息",
                                       new StringType().expand(ConfigMetadataConstants.maxLength, 8096L)),
                                    of("nodeId", "规则节点ID", StringType.GLOBAL),
                                    of("instanceId", "规则实例ID", StringType.GLOBAL)
                                ))
                .then(timeSeriesManager
                              .registerMetadata(
                                      TimeSeriesMetadata
                                              .of(RULE_EVENT_LOG,
                                                  of("createTime", "创建时间", DateTimeType.GLOBAL),
                                                  of("timestamp", "日志时间", DateTimeType.GLOBAL),
                                                  of("event", "事件", StringType.GLOBAL),
                                                  of("ruleData", "数据",
                                                     new StringType().expand(ConfigMetadataConstants.maxLength, 8096L)),
                                                  of("nodeId", "规则节点ID", StringType.GLOBAL),
                                                  of("instanceId", "规则实例ID", StringType.GLOBAL)
                                              ))
                )
                .subscribe();
    }
}
