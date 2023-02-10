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
