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
package org.jetlinks.community.rule.engine.io;

import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.scope.GlobalScope;
import org.jetlinks.rule.engine.api.task.CompositeOutput;
import org.jetlinks.rule.engine.api.task.ConditionEvaluator;
import org.jetlinks.rule.engine.api.task.Input;
import org.jetlinks.rule.engine.api.task.Output;
import org.jetlinks.rule.engine.cluster.scope.ClusterGlobalScope;
import org.jetlinks.rule.engine.cluster.worker.RuleIOManager;
import org.jetlinks.rule.engine.defaults.EventBusEventOutput;
import org.jetlinks.rule.engine.defaults.EventBusOutput;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventBusRuleIOManager implements RuleIOManager {
    private final EventBus eventBus;

    private final ConditionEvaluator conditionEvaluator;

    private final GlobalScope globalScope;

    static {
        RecyclerUtils.share("_r_e_in");
        RecyclerUtils.share("rule-engine");
    }

    static final SharedPathString baseEventAddress = SharedPathString.of("/_r_e_in/*/*");

    public EventBusRuleIOManager(EventBus eventBus, ConditionEvaluator conditionEvaluator, ClusterManager clusterManager) {
        this.eventBus = eventBus;
        this.conditionEvaluator = conditionEvaluator;
        this.globalScope = new ClusterGlobalScope(clusterManager);
    }

    @Override
    public Input createInput(ScheduleJob job) {
        String instanceId = job.getInstanceId();
        String nodeId = job.getNodeId();
        return new Input() {
            @Override
            public Flux<RuleData> accept() {
                return eventBus.subscribe(
                    Subscription
                        .builder()
                        .subscriberId("rule-engine:" + instanceId + ":" + nodeId)
                        .topics(createAddress(instanceId, nodeId))
                        .features(Subscription.Feature.clusterSharedLocalFirstFeatures)
                        .build(),
                    RuleData.class);
            }

            @Override
            public Disposable accept(Function<RuleData, Mono<Boolean>> listener) {
                return eventBus.subscribe(
                    Subscription
                        .builder()
                        .subscriberId("rule-engine:" + instanceId + ":" + nodeId)
                        .topics(createAddress(instanceId, nodeId))
                        .features(Subscription.Feature.clusterSharedLocalFirstFeatures)
                        .build(),
                    data -> listener.apply(data.decode(RuleData.class)).then());
            }
        };
    }

    @Override
    public Output createOutput(ScheduleJob job) {
        return new EventBusOutput(job.getInstanceId(), eventBus, job.getOutputs(), conditionEvaluator) {
            @Override
            protected CharSequence createOutputAddress(String nodeId) {
                return createAddress(instanceId, nodeId);
            }
        };
    }

    private CharSequence createAddress(String instanceId, String nodeId) {
        //   /_r_e_in/{instanceId}/{nodeId}
        return baseEventAddress.replace(2, instanceId, 3, nodeId);
    }

    @Override
    public GlobalScope createScope() {
        return globalScope;
    }

    @Override
    public Map<String, Output> createEvent(ScheduleJob job) {
        String instanceId = job.getInstanceId();
        if (CollectionUtils.isEmpty(job.getEventOutputs())) {
            return Collections.emptyMap();
        }
        return job
            .getEventOutputs()
            .stream()
            .map(event -> new EventBusEventOutput(instanceId, eventBus, event.getType(), event.getSource()) {
                @Override
                protected CharSequence createTopic(String node) {
                    return createAddress(instanceId, node);
                }
            })
            .collect(Collectors.groupingBy(EventBusEventOutput::getEvent, Collectors.collectingAndThen(Collectors.toList(), CompositeOutput::of)));
    }
}
