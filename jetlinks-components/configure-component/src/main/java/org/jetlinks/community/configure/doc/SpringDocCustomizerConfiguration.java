package org.jetlinks.community.configure.doc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
    public OpenAPI springDocCustomizer() {
        return new OpenAPI()
            .info(new Info().title("jetlinks")
                            .contact(new Contact())
                            .description("jetlinks平台API")
                            .version("2.10"));
    }

    @Bean
    public ResponseSupportConverter responseSupportConverter(EntityFactory entityFactory,
                                                             ObjectMapperProvider springDocObjectMapper) {
        return new ResponseWrapperConverter(entityFactory, springDocObjectMapper);
    }

}
