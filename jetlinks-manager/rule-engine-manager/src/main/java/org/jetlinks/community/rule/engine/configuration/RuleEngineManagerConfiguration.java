package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.community.rule.engine.scene.SceneFilter;
import org.jetlinks.community.rule.engine.scene.SceneTaskExecutorProvider;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RuleEngineManagerConfiguration {


    @Bean
    public SceneTaskExecutorProvider sceneTaskExecutorProvider(EventBus eventBus,
                                                               ObjectProvider<SceneFilter> filters) {
        return new SceneTaskExecutorProvider(eventBus,
                                             SceneFilter.composite(filters));
    }
}
