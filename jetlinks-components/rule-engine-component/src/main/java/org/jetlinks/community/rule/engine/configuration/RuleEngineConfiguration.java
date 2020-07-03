package org.jetlinks.community.rule.engine.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.rule.engine.api.EventBus;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.rpc.RpcService;
import org.jetlinks.rule.engine.api.rpc.RpcServiceFactory;
import org.jetlinks.rule.engine.api.scheduler.Scheduler;
import org.jetlinks.rule.engine.api.task.ConditionEvaluator;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.api.worker.Worker;
import org.jetlinks.rule.engine.condition.ConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.DefaultConditionEvaluator;
import org.jetlinks.rule.engine.condition.supports.DefaultScriptEvaluator;
import org.jetlinks.rule.engine.condition.supports.ScriptConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.supports.ScriptEvaluator;
import org.jetlinks.rule.engine.defaults.DefaultRuleEngine;
import org.jetlinks.rule.engine.defaults.LocalEventBus;
import org.jetlinks.rule.engine.defaults.LocalScheduler;
import org.jetlinks.rule.engine.defaults.LocalWorker;
import org.jetlinks.rule.engine.defaults.rpc.DefaultRpcServiceFactory;
import org.jetlinks.rule.engine.defaults.rpc.EventBusRcpService;
import org.jetlinks.rule.engine.model.DefaultRuleModelParser;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.jetlinks.rule.engine.model.antv.AntVG6RuleModelParserStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RuleEngineConfiguration {

    @Bean
    public DefaultRuleModelParser defaultRuleModelParser() {
        return new DefaultRuleModelParser();
    }

    @Bean
    public DefaultConditionEvaluator defaultConditionEvaluator() {
        return new DefaultConditionEvaluator();
    }

    @Bean
    public AntVG6RuleModelParserStrategy antVG6RuleModelParserStrategy() {
        return new AntVG6RuleModelParserStrategy();
    }

    @Bean
    public EventBus eventBus(MessageGateway messageGateway) {

        LocalEventBus local = new LocalEventBus();

        //转发到消息网关
        local.subscribe("/**")
            .flatMap(subscribePayload -> messageGateway.publish(new EventTopicMessage(subscribePayload)).then())
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), obj))
            .subscribe();

        return local;
    }

    @Bean
    public RpcService rpcService(EventBus eventBus) {
        return new EventBusRcpService(eventBus);
    }

    @Bean
    public RpcServiceFactory rpcServiceFactory(RpcService rpcService) {
        return new DefaultRpcServiceFactory(rpcService);
    }

    @Bean
    public Scheduler localScheduler(Worker worker) {
        LocalScheduler scheduler = new LocalScheduler("local");
        scheduler.addWorker(worker);
        return scheduler;
    }

    @Bean
    public BeanPostProcessor autoRegisterStrategy(DefaultRuleModelParser defaultRuleModelParser,
                                                  DefaultConditionEvaluator defaultConditionEvaluator,
                                                  LocalWorker worker) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RuleModelParserStrategy) {
                    defaultRuleModelParser.register(((RuleModelParserStrategy) bean));
                }
                if (bean instanceof ConditionEvaluatorStrategy) {
                    defaultConditionEvaluator.register(((ConditionEvaluatorStrategy) bean));
                }
                if (bean instanceof TaskExecutorProvider) {
                    worker.addExecutor(((TaskExecutorProvider) bean));
                }

                return bean;
            }
        };
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
    public LocalWorker localWorker(EventBus eventBus, ConditionEvaluator evaluator) {
        return new LocalWorker("local", "local", eventBus, evaluator);
    }


    @Bean
    public RuleEngine defaultRuleEngine(Scheduler scheduler) {
        return new DefaultRuleEngine(scheduler);
    }

}
