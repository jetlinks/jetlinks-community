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
package org.jetlinks.community.elastic.search.configuration;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.things.ElasticSearchColumnModeStrategy;
import org.jetlinks.community.elastic.search.things.ElasticSearchRowModeStrategy;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ThingsDataRepositoryStrategy.class)
public class ElasticSearchThingDataConfiguration {

    @Bean
    @ConditionalOnBean(ElasticSearchService.class)
    public ElasticSearchColumnModeStrategy elasticSearchColumnModThingDataPolicy(
        ThingsRegistry registry,
        ElasticSearchService searchService,
        AggregationService aggregationService,
        ElasticSearchIndexManager indexManager) {

        return new ElasticSearchColumnModeStrategy(registry, searchService, aggregationService, indexManager);
    }

    @Bean
    @ConditionalOnBean(ElasticSearchService.class)
    public ElasticSearchRowModeStrategy elasticSearchRowModThingDataPolicy(
        ThingsRegistry registry,
        ElasticSearchService searchService,
        AggregationService aggregationService,
        ElasticSearchIndexManager indexManager) {

        return new ElasticSearchRowModeStrategy(registry, searchService, aggregationService, indexManager);
    }
}
