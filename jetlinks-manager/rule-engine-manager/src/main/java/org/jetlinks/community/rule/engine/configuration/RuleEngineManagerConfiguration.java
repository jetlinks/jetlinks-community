package org.jetlinks.community.rule.engine.configuration;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.alarm.AlarmHandler;
import org.jetlinks.community.rule.engine.cmd.*;
import org.jetlinks.community.rule.engine.entity.AlarmLevelEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRuleBindEntity;
import org.jetlinks.community.rule.engine.scene.*;
import org.jetlinks.community.rule.engine.service.*;
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

    @Bean
    @ConditionalOnClass(AlarmRuleBindEntity.class)
    public RuleCommandSupportManager alarmRuleBindCommandSupportManager(SceneService sceneService,
                                                                        AlarmConfigService configService,
                                                                        ReactiveRepository<AlarmLevelEntity, String> alarmLevelRepository,
                                                                        AlarmRecordService recordService,
                                                                        AlarmHandleHistoryService handleHistoryService,
                                                                        AlarmRuleBindService ruleBindService,
                                                                        AlarmHistoryService historyService,
                                                                        AlarmHandler alarmHandler) {
        return new RuleCommandSupportManager(
            new SceneCommandSupport(sceneService),
            new AlarmHistoryCommandSupport(historyService),
            new AlarmConfigCommandSupport(configService, alarmLevelRepository),
            new AlarmRecordCommandSupport(recordService, handleHistoryService),
            new AlarmRuleBindCommandSupport(ruleBindService),
            new AlarmCommandSupport(alarmHandler)
        );
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ElasticSearchService.class)
    static class ElasticSearchAlarmHistoryConfiguration {

        @Bean(initMethod = "init")
        public ElasticSearchAlarmHistoryService alarmHistoryService(ElasticSearchService elasticSearchService,
                                                                    ElasticSearchIndexManager indexManager,
                                                                    AggregationService aggregationService) {
            return new ElasticSearchAlarmHistoryService(indexManager, elasticSearchService, aggregationService);
        }
    }
}
