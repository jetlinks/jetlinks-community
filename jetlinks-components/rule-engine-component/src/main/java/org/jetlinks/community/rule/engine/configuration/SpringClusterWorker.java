package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.cluster.worker.ClusterWorker;
import org.jetlinks.rule.engine.cluster.worker.RuleIOManager;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

public class SpringClusterWorker extends ClusterWorker implements SmartInitializingSingleton {
    private final ApplicationContext context;

    public SpringClusterWorker(String id,
                               String name,
                               EventBus eventBus,
                               RuleIOManager ioManager,
                               ApplicationContext context) {
        super(id, name, eventBus, ioManager);
        this.context = context;
    }

    @Override
    public void afterSingletonsInstantiated() {
        context
            .getBeansOfType(TaskExecutorProvider.class)
            .forEach((beanName, provider) -> addExecutor(provider));
    }
}
