package org.jetlinks.community.configure.doc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.reactivestreams.Publisher;
import org.springdoc.core.converters.ResponseSupportConverter;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webflux.core.configuration.SpringDocWebFluxConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.List;

import static org.springdoc.core.converters.ConverterUtils.isResponseTypeToIgnore;

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
    public ResponseSupportConverter responseSupportConverter(ObjectMapperProvider springDocObjectMapper) {
        return new ResponseSupportConverter(springDocObjectMapper) {
            @Override
            public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
                JavaType javaType = springDocObjectMapper.jsonMapper().constructType(type.getType());
                if (javaType != null) {
                    Class<?> rawClass = javaType.getRawClass();
                    if (rawClass != null && Publisher.class.isAssignableFrom(rawClass)) {
                        JavaType actualType = javaType.containedType(0);
                        if (actualType != null) {
                            if (actualType.getRawClass() == ResponseEntity.class ||
                                actualType.getRawClass() == ResponseMessage.class) {
                                return super.resolve(type, context, chain);
                            }
                            JavaType wrappedType = buildWrappedType(actualType, rawClass);
                            return resolveWrappedType(context, chain, type, wrappedType);
                        }
                    } else if (isResponseTypeToIgnore(rawClass)) {
                        return null;
                    }
                }
                return continueResolve(type, context, chain);
            }

            private JavaType buildWrappedType(JavaType actualType, Class<?> rawClass) {
                TypeFactory tf = springDocObjectMapper.jsonMapper().getTypeFactory();
                boolean isFlux = Flux.class.isAssignableFrom(rawClass);
                try {
                    JavaType baseType = isFlux ? tf.constructCollectionType(List.class, actualType) : actualType;
                    JavaType responseMessageType = tf.constructParametricType(ResponseMessage.class, baseType);
                    return tf.constructParametricType(rawClass, responseMessageType);
                } catch (IllegalArgumentException ignore) {
                    return null;
                }
            }

            private Schema resolveWrappedType(ModelConverterContext context,
                                              Iterator<ModelConverter> chain,
                                              AnnotatedType originalType,
                                              JavaType wrappedType) {
                if (wrappedType == null) {
                    return continueResolve(originalType, context, chain);
                }

                AnnotatedType newType = new AnnotatedType()
                    .type(wrappedType)
                    .jsonViewAnnotation(originalType.getJsonViewAnnotation())
                    .ctxAnnotations(originalType.getCtxAnnotations())
                    .resolveAsRef(true);

                return context.resolve(newType);
            }

            private Schema continueResolve(AnnotatedType type,
                                           ModelConverterContext context,
                                           Iterator<ModelConverter> chain) {
                return (chain.hasNext()) ? chain.next().resolve(type, context, chain) : null;
            }
        };
    }

}
