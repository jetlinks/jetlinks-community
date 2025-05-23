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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Generated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.jetlinks.community.elastic.search.index.*;
import org.jetlinks.community.elastic.search.index.strategies.*;
import org.jetlinks.community.elastic.search.service.reactive.*;
import org.jetlinks.community.elastic.search.index.*;
import org.jetlinks.community.elastic.search.index.strategies.*;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.service.reactive.*;
import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ReactiveElasticsearchClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@AutoConfiguration(after = ReactiveElasticsearchClientAutoConfiguration.class)
@Slf4j
@EnableConfigurationProperties({
    ElasticSearchIndexProperties.class,
    ElasticSearchBufferProperties.class})
@Generated
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ElasticSearchConfiguration {


    @Bean
    @SneakyThrows
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DefaultReactiveElasticsearchClient defaultReactiveElasticsearchClient(ElasticsearchClient client) {
        return new DefaultReactiveElasticsearchClient(client);
    }


    @Bean
    public DirectElasticSearchIndexStrategy directElasticSearchIndexStrategy(ReactiveElasticsearchClient elasticsearchClient,
                                                                             ElasticSearchIndexProperties indexProperties) {
        return new DirectElasticSearchIndexStrategy(elasticsearchClient, indexProperties);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TimeByMonthElasticSearchIndexStrategy timeByMonthElasticSearchIndexStrategy(ReactiveElasticsearchClient elasticsearchClient,
                                                                                       ElasticSearchIndexProperties indexProperties) {
        return new TimeByMonthElasticSearchIndexStrategy(elasticsearchClient, indexProperties);
    }

    @Bean
    public TimeByDayElasticSearchIndexStrategy timeByDayElasticSearchIndexStrategy(ReactiveElasticsearchClient elasticsearchClient,
                                                                                   ElasticSearchIndexProperties indexProperties) {
        return new TimeByDayElasticSearchIndexStrategy(elasticsearchClient, indexProperties);
    }

    @Bean
    public TimeByWeekElasticSearchIndexStrategy timeByWeekElasticSearchIndexStrategy(ReactiveElasticsearchClient elasticsearchClient,
                                                                                     ElasticSearchIndexProperties indexProperties){
        return new TimeByWeekElasticSearchIndexStrategy(elasticsearchClient, indexProperties);
    }

    @Bean
    public AffixesElasticSearchIndexStrategy affixesElasticSearchIndexStrategy(ReactiveElasticsearchClient elasticsearchClient,
                                                                               ElasticSearchIndexProperties indexProperties) {
        return new AffixesElasticSearchIndexStrategy(elasticsearchClient, indexProperties);
    }

    @Bean
    public DefaultElasticSearchIndexManager elasticSearchIndexManager(@Autowired(required = false) List<ElasticSearchIndexStrategy> strategies,
                                                                      @Autowired(required = false) List<ElasticSearchIndexCustomizer> customizers) {
        return new DefaultElasticSearchIndexManager(strategies, customizers);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE + 1000)
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(ElasticSearchService.class)
    @DependsOn("defaultReactiveElasticsearchClient")
    public ReactiveElasticSearchService reactiveElasticSearchService(ReactiveElasticsearchClient elasticsearchClient,
                                                                     ElasticSearchIndexManager indexManager,
                                                                     ElasticSearchBufferProperties properties) {
        return new ReactiveElasticSearchService(elasticsearchClient, indexManager, properties);
    }

    @Bean
    public ReactiveAggregationService reactiveAggregationService(ElasticSearchIndexManager indexManager,
                                                                 ReactiveElasticsearchClient restClient) {
        return new ReactiveAggregationService(indexManager, restClient);
    }

    @Bean
    public ElasticSearchTimeSeriesManager elasticSearchTimeSeriesManager(ElasticSearchIndexManager indexManager,
                                                                         ElasticSearchService elasticSearchService,
                                                                         AggregationService aggregationService) {
        return new ElasticSearchTimeSeriesManager(indexManager, elasticSearchService, aggregationService);
    }

}
