package org.jetlinks.community.elastic.search.configuration;

import lombok.Generated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.elastic.search.embedded.EmbeddedElasticSearch;
import org.jetlinks.community.elastic.search.embedded.EmbeddedElasticSearchProperties;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexStrategy;
import org.jetlinks.community.elastic.search.index.strategies.DirectElasticSearchIndexStrategy;
import org.jetlinks.community.elastic.search.index.strategies.TimeByMonthElasticSearchIndexStrategy;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.service.reactive.*;
import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.HostProvider;
import org.springframework.data.elasticsearch.client.reactive.RequestCreator;
import org.springframework.data.elasticsearch.client.reactive.WebClientProvider;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Configuration(proxyBeanMethods = false)
@Slf4j
@EnableConfigurationProperties({
    EmbeddedElasticSearchProperties.class,
    ElasticSearchIndexProperties.class,
    ElasticSearchBufferProperties.class})
@AutoConfigureAfter(ReactiveElasticsearchRestClientAutoConfiguration.class)
@ConditionalOnBean(ClientConfiguration.class)
@Generated
public class ElasticSearchConfiguration {

    @Bean
    @SneakyThrows
    @Primary
    public DefaultReactiveElasticsearchClient defaultReactiveElasticsearchClient(EmbeddedElasticSearchProperties embeddedProperties,
                                                                                 ClientConfiguration clientConfiguration) {
        if (embeddedProperties.isEnabled()) {
            log.debug("starting embedded elasticsearch on {}:{}",
                      embeddedProperties.getHost(),
                      embeddedProperties.getPort());

            new EmbeddedElasticSearch(embeddedProperties).start();
        }
        WebClientProvider provider = getWebClientProvider(clientConfiguration);

        HostProvider<?> hostProvider = HostProvider.provider(provider,
                                                          clientConfiguration.getHeadersSupplier(),
                                                          clientConfiguration
                                                              .getEndpoints()
                                                              .toArray(new InetSocketAddress[0]));

        DefaultReactiveElasticsearchClient client =
            new DefaultReactiveElasticsearchClient(hostProvider, new RequestCreator() {
            });

        client.setHeadersSupplier(clientConfiguration.getHeadersSupplier());

        return client;
    }

    private static WebClientProvider getWebClientProvider(ClientConfiguration clientConfiguration) {

        return WebClientProvider.getWebClientProvider(clientConfiguration);
    }

    @Bean
    public DirectElasticSearchIndexStrategy directElasticSearchIndexStrategy(ReactiveElasticsearchClient elasticsearchClient,
                                                                             ElasticSearchIndexProperties indexProperties) {
        return new DirectElasticSearchIndexStrategy(elasticsearchClient, indexProperties);
    }

    @Bean
    public TimeByMonthElasticSearchIndexStrategy timeByMonthElasticSearchIndexStrategy(ReactiveElasticsearchClient elasticsearchClient,
                                                                                       ElasticSearchIndexProperties indexProperties) {
        return new TimeByMonthElasticSearchIndexStrategy(elasticsearchClient, indexProperties);
    }

    @Bean
    public DefaultElasticSearchIndexManager elasticSearchIndexManager(@Autowired(required = false) List<ElasticSearchIndexStrategy> strategies) {
        return new DefaultElasticSearchIndexManager(strategies);
    }

    @Bean
    @ConditionalOnMissingBean(ElasticSearchService.class)
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
