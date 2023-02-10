package org.jetlinks.community.elastic.search.configuration;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.things.ElasticSearchColumnModeStrategy;
import org.jetlinks.community.elastic.search.things.ElasticSearchRowModeStrategy;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ThingsDataRepositoryStrategy.class)
public class ElasticSearchThingDataConfiguration {

    @Bean
    public ElasticSearchColumnModeStrategy elasticSearchColumnModThingDataPolicy(
        ThingsRegistry registry,
        ElasticSearchService searchService,
        AggregationService aggregationService,
        ElasticSearchIndexManager indexManager) {

        return new ElasticSearchColumnModeStrategy(registry, searchService, aggregationService, indexManager);
    }

    @Bean
    public ElasticSearchRowModeStrategy elasticSearchRowModThingDataPolicy(
        ThingsRegistry registry,
        ElasticSearchService searchService,
        AggregationService aggregationService,
        ElasticSearchIndexManager indexManager) {

        return new ElasticSearchRowModeStrategy(registry, searchService, aggregationService, indexManager);
    }
}
