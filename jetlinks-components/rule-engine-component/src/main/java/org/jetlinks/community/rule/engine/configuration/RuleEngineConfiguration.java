package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.community.rule.engine.nodes.TimerWorkerNode;
import org.jetlinks.rule.engine.api.ConditionEvaluator;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.Slf4jLogger;
import org.jetlinks.rule.engine.api.executor.ExecutableRuleNodeFactory;
import org.jetlinks.rule.engine.cluster.logger.ClusterLogger;
import org.jetlinks.rule.engine.condition.ConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.DefaultConditionEvaluator;
import org.jetlinks.rule.engine.condition.supports.DefaultScriptEvaluator;
import org.jetlinks.rule.engine.condition.supports.ScriptConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.supports.ScriptEvaluator;
import org.jetlinks.rule.engine.executor.DefaultExecutableRuleNodeFactory;
import org.jetlinks.rule.engine.executor.ExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.node.route.RouteEventNode;
import org.jetlinks.rule.engine.model.DefaultRuleModelParser;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.jetlinks.rule.engine.model.antv.AntVG6RuleModelParserStrategy;
import org.jetlinks.rule.engine.standalone.StandaloneRuleEngine;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

@Configuration
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
    public DefaultExecutableRuleNodeFactory defaultExecutableRuleNodeFactory() {
        return new DefaultExecutableRuleNodeFactory();
    }

    @Bean
    public AntVG6RuleModelParserStrategy antVG6RuleModelParserStrategy() {
        return new AntVG6RuleModelParserStrategy();
    }

    @Bean
    public BeanPostProcessor autoRegisterStrategy(DefaultRuleModelParser defaultRuleModelParser,
                                                  DefaultConditionEvaluator defaultConditionEvaluator,
                                                  DefaultExecutableRuleNodeFactory ruleNodeFactory) {
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
                if (bean instanceof ExecutableRuleNodeFactoryStrategy) {
                    ruleNodeFactory.registerStrategy(((ExecutableRuleNodeFactoryStrategy) bean));
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
    public RuleEngine ruleEngine(ExecutableRuleNodeFactory ruleNodeFactory,
                                 ConditionEvaluator conditionEvaluator,
                                 ApplicationEventPublisher eventPublisher,
                                 ExecutorService executorService) {
        StandaloneRuleEngine ruleEngine = new StandaloneRuleEngine();
        ruleEngine.setNodeFactory(ruleNodeFactory);
        ruleEngine.setExecutor(executorService);
        ruleEngine.setEvaluator(conditionEvaluator);
        ruleEngine.setEventListener(eventPublisher::publishEvent);
        ruleEngine.setLoggerSupplier((ctxId, model) -> {
            ClusterLogger logger = new ClusterLogger();
            logger.setParent(new Slf4jLogger("rule.engine.logger.".concat(model.getId()).concat(".").concat(model.getName())));
            logger.setLogInfoConsumer(eventPublisher::publishEvent);
            logger.setNodeId(model.getId());
            logger.setInstanceId(ctxId);
            return logger;
        });
        return ruleEngine;
    }

    /* 规则引擎节点 */

    @Bean //定时调度
    public TimerWorkerNode timerWorkerNode() {
        return new TimerWorkerNode();
    }

    @Bean
    public RouteEventNode routeEventNode() {
        return new RouteEventNode();
    }


}
