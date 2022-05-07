package org.jetlinks.community.configure.doc;

import org.hswebframework.web.api.crud.entity.EntityFactory;
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

@Configuration
@AutoConfigureBefore(SpringDocWebFluxConfiguration.class)
public class SpringDocCustomizerConfiguration {

    @Bean
    public ReturnTypeParser operationCustomizer(EntityFactory factory) {

        return new ReturnTypeParser() {
            @Override
            public Type getReturnType(MethodParameter methodParameter) {
                Type type = ReturnTypeParser.super.getReturnType(methodParameter);

                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = ((ParameterizedType) type);
                    Type rawType = parameterizedType.getRawType();
                    if (rawType instanceof Class && Publisher.class.isAssignableFrom(((Class<?>) rawType))) {
                        Type actualType = parameterizedType.getActualTypeArguments()[0];

                        if (actualType instanceof ParameterizedType) {
                            actualType = ((ParameterizedType) actualType).getRawType();
                        }
                        if (actualType == ResponseEntity.class || actualType == ResponseMessage.class) {
                            return type;
                        }
                        boolean returnList = Flux.class.isAssignableFrom(((Class<?>) rawType));

                        //统一返回ResponseMessage
                        return ResolvableType
                            .forClassWithGenerics(
                                Mono.class,
                                ResolvableType.forClassWithGenerics(
                                   factory.getInstanceType(ResponseMessage.class),
                                    returnList ?
                                        ResolvableType.forClassWithGenerics(
                                            List.class,
                                            ResolvableType.forType(parameterizedType.getActualTypeArguments()[0])
                                        ) :
                                        ResolvableType.forType(parameterizedType.getActualTypeArguments()[0])
                                ))
                            .getType();

                    }
                }

                return type;
            }
        };
    }
}
