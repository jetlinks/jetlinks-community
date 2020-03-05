package org.jetlinks.community.elastic.search.configuration;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Configuration
@Slf4j
@EnableConfigurationProperties(ElasticSearchProperties.class)
public class ElasticSearchConfiguration {

    @Autowired
    private ElasticSearchProperties properties;

    @Bean
    public ElasticRestClient elasticRestClient() {
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(properties.createHosts())
            .setRequestConfigCallback(properties::applyRequestConfigBuilder)
            .setHttpClientConfigCallback(properties::applyHttpAsyncClientBuilder));
        return new ElasticRestClient(client, client);
    }
}
