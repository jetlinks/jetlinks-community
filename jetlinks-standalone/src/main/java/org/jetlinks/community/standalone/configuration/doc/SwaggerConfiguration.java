package org.jetlinks.community.standalone.configuration.doc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.reactivestreams.Publisher;
import org.springdoc.core.ReturnTypeParser;
import org.springdoc.webflux.core.SpringDocWebFluxConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
    info = @Info(
        title = "物联网平台",
        description = "物联网平台接口文档",
        contact = @Contact(name = "admin",url = "https://github.com/jetlinks"),
        version = "1.12.0"
    )
)
@SecuritySchemes(
    {
        @SecurityScheme(
            type = SecuritySchemeType.APIKEY,
            name = "Token",
            paramName = "X-Access-Token",
            in = SecuritySchemeIn.HEADER,
            description = "认证token"
        )
    }
)
@AutoConfigureBefore(SpringDocWebFluxConfiguration.class)
public class SwaggerConfiguration {

}
