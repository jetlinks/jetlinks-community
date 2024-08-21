package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.scene.*;
import org.jetlinks.community.rule.engine.service.ElasticSearchAlarmHistoryService;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
public class RuleEngineManagerConfiguration {

    @Bean
    public SceneTaskExecutorProvider sceneTaskExecutorProvider(EventBus eventBus,
                                                               ObjectProvider<SceneFilter> filters,
                                                               ObjectProvider<SceneActionProvider<?>> providers,
                                                               ObjectProvider<SceneTriggerProvider<?>> triggerProviders) {
        providers.forEach(SceneProviders::register);
        triggerProviders.forEach(SceneProviders::register);
        return new SceneTaskExecutorProvider(eventBus,
                                             SceneFilter.composite(filters));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ElasticSearchService.class)
    static class ElasticSearchAlarmHistoryConfiguration {

        @Bean(initMethod = "init")
        public ElasticSearchAlarmHistoryService alarmHistoryService(ElasticSearchService elasticSearchService,
                                                                    ElasticSearchIndexManager indexManager) {
            return new ElasticSearchAlarmHistoryService(indexManager, elasticSearchService);
        }
    }
}
