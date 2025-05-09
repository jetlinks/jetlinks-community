package org.jetlinks.community.configure.doc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.hswebframework.web.api.crud.entity.EntityFactory;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.reactivestreams.Publisher;
import org.springdoc.core.converters.ResponseSupportConverter;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.List;

import static org.springdoc.core.converters.ConverterUtils.isResponseTypeToIgnore;

public class ResponseWrapperConverter extends ResponseSupportConverter {

    private final ObjectMapperProvider mapperProvider;

    private final EntityFactory entityFactory;

    public ResponseWrapperConverter(ObjectMapperProvider mapperProvider,
                                    EntityFactory entityFactory) {
        super(mapperProvider);
        this.mapperProvider = mapperProvider;
        this.entityFactory = entityFactory;
    }

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = mapperProvider.jsonMapper().constructType(type.getType());
        if (javaType != null) {
            Class<?> rawClass = entityFactory.getInstanceType(javaType.getRawClass());
            if (rawClass == null) {
                rawClass = javaType.getRawClass();
            }
            if (isResponseTypeToIgnore(rawClass) || rawClass == null) {
                return null;
            } else if (Publisher.class.isAssignableFrom(rawClass)) {
                JavaType actualType = javaType.containedType(0);
                if (actualType != null) {
                    if (actualType.getRawClass() == ResponseEntity.class ||
                        actualType.getRawClass() == ResponseMessage.class) {
                        return super.resolve(type, context, chain);
                    }
                    JavaType wrappedType = buildWrappedType(actualType, rawClass);
                    return resolveWrappedType(context, chain, type, wrappedType);
                }
            } else if (javaType.getRawClass() == ResponseEntity.class ||
                javaType.getRawClass() == ResponseMessage.class) {
                return resolveWrappedType(context, chain, type, javaType);
            } else {
                JavaType jt = mapperProvider.
                    jsonMapper()
                    .getTypeFactory()
                    .constructParametricType(entityFactory.getInstanceType(ResponseMessage.class), javaType);
                return resolveWrappedType(context, chain, type, jt);
            }
        }
        return continueResolve(type, context, chain);
    }

    private JavaType buildWrappedType(JavaType actualType, Class<?> rawClass) {
        TypeFactory tf = mapperProvider.jsonMapper().getTypeFactory();
        boolean isFlux = Flux.class.isAssignableFrom(rawClass);
        try {
            JavaType baseType = isFlux ? tf.constructCollectionType(List.class, actualType) : actualType;
            JavaType responseMessageType = tf.constructParametricType(entityFactory.getInstanceType(ResponseMessage.class), baseType);
            return tf.constructParametricType(rawClass, responseMessageType);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    private Schema<?> resolveWrappedType(ModelConverterContext context,
                                         Iterator<ModelConverter> chain,
                                         AnnotatedType originalType,
                                         JavaType wrappedType) {
        if (wrappedType == null) {
            return continueResolve(originalType, context, chain);
        }

        return context.resolve(new AnnotatedType()
                                   .type(wrappedType)
                                   .jsonViewAnnotation(originalType.getJsonViewAnnotation())
                                   .ctxAnnotations(originalType.getCtxAnnotations())
                                   .resolveAsRef(true));
    }

    private Schema<?> continueResolve(AnnotatedType type,
                                      ModelConverterContext context,
                                      Iterator<ModelConverter> chain) {
        return (chain.hasNext()) ? chain.next().resolve(type, context, chain) : null;
    }
}
