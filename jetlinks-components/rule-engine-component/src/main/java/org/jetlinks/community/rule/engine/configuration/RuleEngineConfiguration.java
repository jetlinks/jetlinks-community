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
package org.jetlinks.community.rule.engine.configuration;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.configure.device.DeviceClusterConfiguration;
import org.jetlinks.community.rule.engine.cluster.ClusterSchedulerLoadBalancer;
import org.jetlinks.community.rule.engine.commons.ShakeLimitProvider;
import org.jetlinks.community.rule.engine.commons.TermsConditionEvaluator;
import org.jetlinks.community.rule.engine.entity.TaskSnapshotEntity;
import org.jetlinks.community.rule.engine.executor.DeviceSelectorBuilder;
import org.jetlinks.community.rule.engine.executor.device.DeviceDataTaskExecutorProvider;
import org.jetlinks.community.rule.engine.io.EventBusRuleIOManager;
import org.jetlinks.community.rule.engine.log.TimeSeriesRuleEngineLogService;
import org.jetlinks.community.rule.engine.repository.LocalTaskSnapshotRepository;
import org.jetlinks.community.things.configuration.ThingsConfiguration;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.scheduler.Scheduler;
import org.jetlinks.rule.engine.api.scheduler.SchedulerSelector;
import org.jetlinks.rule.engine.api.task.ConditionEvaluator;
import org.jetlinks.rule.engine.api.worker.Worker;
import org.jetlinks.rule.engine.cluster.ClusterRuleEngine;
import org.jetlinks.rule.engine.cluster.RuleInstanceRepository;
import org.jetlinks.rule.engine.cluster.SchedulerRegistry;
import org.jetlinks.rule.engine.cluster.TaskSnapshotRepository;
import org.jetlinks.rule.engine.cluster.scheduler.ClusterRpcSchedulerRegistry;
import org.jetlinks.rule.engine.condition.ConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.DefaultConditionEvaluator;
import org.jetlinks.rule.engine.condition.supports.DefaultScriptEvaluator;
import org.jetlinks.rule.engine.condition.supports.ScriptConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.supports.ScriptEvaluator;
import org.jetlinks.rule.engine.defaults.LocalScheduler;
import org.jetlinks.rule.engine.model.DefaultRuleModelParser;
import org.jetlinks.rule.engine.model.antv.AntVG6RuleModelParserStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {DeviceClusterConfiguration.class, ThingsConfiguration.class})
@Slf4j
@EnableConfigurationProperties(RuleEngineProperties.class)
public class RuleEngineConfiguration {

    static {
        RuleData.create("load-serializer");
    }

    @Bean
    public DefaultRuleModelParser defaultRuleModelParser() {
        return new DefaultRuleModelParser();
    }

    @Bean
    public TermsConditionEvaluator termsConditionEvaluator() {
        return new TermsConditionEvaluator();
    }

    @Bean
    public DefaultConditionEvaluator defaultConditionEvaluator(ObjectProvider<ConditionEvaluatorStrategy> strategies) {
        DefaultConditionEvaluator evaluator =  new DefaultConditionEvaluator();
        strategies.forEach(evaluator::register);
        return evaluator;
    }


    @Bean(destroyMethod = "dispose")
    public LocalScheduler ruleScheduler(RuleEngineProperties properties,
                                        ObjectProvider<Worker> workers) {
        LocalScheduler scheduler = new LocalScheduler(properties.getServerId());
        workers.forEach(scheduler::addWorker);
        return scheduler;
    }


    @Bean
    public SchedulerSelector schedulerSelector() {
        return SchedulerSelector.selectAll;
    }


    @Bean
    public SchedulerRegistry schedulerRegistry(RpcManager rpcManager,
                                               ObjectProvider<Scheduler> schedulers,
                                               RuleEngineProperties properties) {
        ClusterRpcSchedulerRegistry registry = new ClusterRpcSchedulerRegistry(properties.getNamespace(), rpcManager);

        schedulers.forEach(registry::register);

        return registry;
    }


    @Bean
    public TaskSnapshotRepository taskSnapshotRepository(ReactiveRepository<TaskSnapshotEntity, String> repository) {
        return new LocalTaskSnapshotRepository(repository);
    }

    @Bean
    @ConditionalOnBean({
        RuleInstanceRepository.class,
        SchedulerRegistry.class
    })
    public ClusterSchedulerLoadBalancer clusterSchedulerLoadBalancer(EventBus eventBus,
                                                                     SchedulerRegistry registry,
                                                                     TaskSnapshotRepository taskSnapshotRepository,
                                                                     RuleInstanceRepository instanceRepository,
                                                                     SchedulerSelector schedulerSelector) {
        return new ClusterSchedulerLoadBalancer(eventBus, registry,
                                                taskSnapshotRepository,
                                                instanceRepository,
                                                schedulerSelector);
    }

    @Bean
    public ScriptEvaluator ruleEngineScriptEvaluator() {
        return new DefaultScriptEvaluator();
    }

    @Bean
    public ScriptConditionEvaluatorStrategy scriptConditionEvaluatorStrategy(ScriptEvaluator scriptEvaluator) {
        return new ScriptConditionEvaluatorStrategy(scriptEvaluator);
    }

    @Bean
    public SpringClusterWorker clusterWorker(RuleEngineProperties properties,
                                             EventBus eventBus,
                                             ClusterManager clusterManager,
                                             ConditionEvaluator evaluator,
                                             ApplicationContext context) {
        return new SpringClusterWorker(properties.getServerId(),
                                       properties.getServerName(),
                                       eventBus,
                                       new EventBusRuleIOManager(eventBus, evaluator, clusterManager),
                                       context);

    }

    @Bean
    @ConditionalOnBean({
        SchedulerRegistry.class,
        SchedulerSelector.class,
        TaskSnapshotRepository.class,
    })
    public RuleEngine ruleEngine(SchedulerRegistry registry,
                                 TaskSnapshotRepository repository,
                                 SchedulerSelector selector) {
        return new ClusterRuleEngine(registry, repository, selector);
    }

    @Bean
    public DeviceDataTaskExecutorProvider deviceDataTaskExecutorProvider(ThingsDataManager dataManager,
                                                                         DeviceSelectorBuilder selectorBuilder) {
        return new DeviceDataTaskExecutorProvider(dataManager, selectorBuilder);
    }

    @Bean
    public TimeSeriesRuleEngineLogService ruleEngineLogService(TimeSeriesManager timeSeriesManager) {
        return new TimeSeriesRuleEngineLogService(timeSeriesManager);
    }

    @Bean
    public SmartInitializingSingleton shakeLimitProviderRegister(ApplicationContext context) {
        return () -> context
            .getBeansOfType(ShakeLimitProvider.class)
            .values()
            .forEach(provider -> ShakeLimitProvider.supports.register(provider.provider(), provider));
    }

}
