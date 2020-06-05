package org.jetlinks.community.elastic.search.configuration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.embedded.EmbeddedElasticSearch;
import org.jetlinks.community.elastic.search.embedded.EmbeddedElasticSearchProperties;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Configuration
@Slf4j
@EnableConfigurationProperties({
    ElasticSearchProperties.class,
    EmbeddedElasticSearchProperties.class,
    ElasticSearchIndexProperties.class})
public class ElasticSearchConfiguration {

    private final ElasticSearchProperties properties;

    private final EmbeddedElasticSearchProperties embeddedProperties;

    public ElasticSearchConfiguration(ElasticSearchProperties properties, EmbeddedElasticSearchProperties embeddedProperties) {
        this.properties = properties;
        this.embeddedProperties = embeddedProperties;
    }

    @Bean
    @SneakyThrows
    public ElasticRestClient elasticRestClient() {
        if (embeddedProperties.isEnabled()) {
            log.debug("starting embedded elasticsearch on {}:{}",
                embeddedProperties.getHost(),
                embeddedProperties.getPort());

            new EmbeddedElasticSearch(embeddedProperties)
                .start();
        }
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(properties.createHosts())
            .setRequestConfigCallback(properties::applyRequestConfigBuilder)
            .setHttpClientConfigCallback(properties::applyHttpAsyncClientBuilder));
        return new ElasticRestClient(client, client);
    }

    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient(ElasticRestClient client) {
        return client.getWriteClient();
    }

}
