package org.jetlinks.community.notify.manager.configuration;

import org.jetlinks.community.elastic.search.configuration.ElasticSearchConfiguration;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.notify.manager.service.ElasticSearchNotifyHistoryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ElasticSearchService.class)
@AutoConfigureAfter(ElasticSearchConfiguration.class)
public class ElasticSearchNotifyHistoryConfiguration {

    @Bean(initMethod = "init")
    @ConditionalOnBean(ElasticSearchService.class)
    public ElasticSearchNotifyHistoryRepository notifyHistoryRepository(ElasticSearchService elasticSearchService,
                                                                        ElasticSearchIndexManager indexManager) {
        return new ElasticSearchNotifyHistoryRepository(elasticSearchService, indexManager);
    }
}