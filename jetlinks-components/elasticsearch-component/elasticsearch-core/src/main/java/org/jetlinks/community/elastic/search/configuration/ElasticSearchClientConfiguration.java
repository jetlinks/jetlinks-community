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

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.jetlinks.community.elastic.search.trace.TraceInstrumentation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ReactiveElasticsearchClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * @author zhouhao
 * @since 2.10
 **/
@AutoConfiguration(before = ReactiveElasticsearchClientAutoConfiguration.class)
@Slf4j
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ElasticSearchClientConfiguration {

    @Bean
    JacksonJsonpMapper jacksonJsonpMapper(ObjectMapper mapper) {
        return new JacksonJsonpMapper(mapper);
    }

    @Bean
    RestClientTransport restClientTransport(RestClient restClient, JsonpMapper jsonMapper,
                                            ObjectProvider<RestClientOptions> restClientOptions) {
        return new RestClientTransport(
            restClient,
            jsonMapper,
            restClientOptions.getIfAvailable(),
            new TraceInstrumentation());
    }



}
