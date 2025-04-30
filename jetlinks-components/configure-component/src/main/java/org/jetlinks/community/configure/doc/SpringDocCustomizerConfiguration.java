package org.jetlinks.community.configure.doc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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

}
