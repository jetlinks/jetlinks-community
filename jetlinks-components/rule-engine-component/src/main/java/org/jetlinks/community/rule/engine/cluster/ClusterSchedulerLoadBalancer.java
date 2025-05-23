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
package org.jetlinks.community.rule.engine.cluster;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.rule.engine.api.scheduler.SchedulerSelector;
import org.jetlinks.rule.engine.cluster.RuleInstanceRepository;
import org.jetlinks.rule.engine.cluster.SchedulerRegistry;
import org.jetlinks.rule.engine.cluster.TaskSnapshotRepository;
import org.jetlinks.rule.engine.cluster.balancer.DefaultSchedulerLoadBalancer;
import org.springframework.boot.CommandLineRunner;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.time.Duration;

/**
 * 集群调度负载均衡器，用于对调度器任务进行负载均衡处理
 *
 * @author zhouhao
 * @since 1.3
 */
@Slf4j
@Generated
public class ClusterSchedulerLoadBalancer extends DefaultSchedulerLoadBalancer implements CommandLineRunner {

    private final RuleInstanceRepository instanceRepository;

    public ClusterSchedulerLoadBalancer(EventBus eventBus,
                                        SchedulerRegistry registry,
                                        TaskSnapshotRepository snapshotRepository,
                                        RuleInstanceRepository instanceRepository,
                                        SchedulerSelector selector) {
        super(eventBus, registry, snapshotRepository, selector);
        this.instanceRepository = instanceRepository;
    }

    @Override
    @PreDestroy
    public void cleanup() {
        super.cleanup();
    }

    @Override
    public void run(String... args) {

        this
            .setupAsync()//恢复之前的调度
            .delayElement(Duration.ofMillis(2000))
            .then(
                instanceRepository
                    .findAll()
                    .flatMap(instance -> this
                        //使用本地调度器进行负载,实现集群新增节点时的弹性调度
                        .reBalance(registry.getLocalSchedulers(), instance, true)
                        .onErrorResume(err -> {
                            log.error("Re Balance Rule [{}] error", instance.getId(), err);
                            return Mono.empty();
                        }))
                    .then()
            )
            .as(MonoTracer.create("/rule-engine/scheduler/startup"))
            .subscribe();
    }
}
