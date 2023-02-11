package org.jetlinks.community.notify.configuration;

import lombok.Generated;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.rule.NotifierTaskExecutorProvider;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnClass({
    TaskExecutorProvider.class
})
@Configuration
@Generated
public class RuleEngineNotifierConfiguration {

    @Bean
    @ConditionalOnBean(NotifierManager.class)
    public NotifierTaskExecutorProvider notifierTaskExecutorProvider(NotifierManager notifierManager) {
        return new NotifierTaskExecutorProvider(notifierManager);
    }
}