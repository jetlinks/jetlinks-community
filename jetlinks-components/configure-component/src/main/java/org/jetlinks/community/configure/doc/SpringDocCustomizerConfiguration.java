package org.jetlinks.community.configure.doc;

import org.hswebframework.web.api.crud.entity.EntityFactory;
import org.springdoc.core.converters.ResponseSupportConverter;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webflux.core.configuration.SpringDocWebFluxConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(SpringDocWebFluxConfiguration.class)
public class SpringDocCustomizerConfiguration {

    @Bean
    public ResponseSupportConverter responseSupportConverter(EntityFactory entityFactory,
                                                             ObjectMapperProvider springDocObjectMapper) {
        return new ResponseWrapperConverter(entityFactory, springDocObjectMapper);
    }

}
