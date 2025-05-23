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
package org.jetlinks.community.tdengine;


import org.jetlinks.community.tdengine.restful.RestfulTDEngineQueryOperations;
import org.jetlinks.community.tdengine.restful.SchemalessTDEngineDataWriter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@ConditionalOnProperty(prefix = "tdengine", value = "enabled", havingValue = "true")
@EnableConfigurationProperties(TDengineProperties.class)
public class TDengineConfiguration {

    @Bean(destroyMethod = "dispose")
    @ConditionalOnMissingBean(TDengineOperations.class)
    public TDengineOperations tDengineOperations(TDengineProperties properties) {
        WebClient client = properties.getRestful().createClient();
        SchemalessTDEngineDataWriter writer = new SchemalessTDEngineDataWriter(client,
                                                                               properties.getDatabase(),
                                                                               properties.getBuffer());

        return new DetectTDengineOperations(writer, new RestfulTDEngineQueryOperations(client, properties.getDatabase()));
    }

}
