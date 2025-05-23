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
package org.jetlinks.community.configure.doc;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.ClassUtils;
import org.hswebframework.web.api.crud.entity.EntityFactory;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.reactivestreams.Publisher;
import org.springdoc.core.converters.ResponseSupportConverter;
import org.springdoc.core.customizers.GlobalOperationComponentsCustomizer;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.core.utils.SpringDocAnnotationsUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;
import reactor.core.publisher.Flux;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ResponseWrapperConverter extends ResponseSupportConverter implements GlobalOperationComponentsCustomizer, Ordered {

    private final EntityFactory entityFactory;

    private final ObjectMapperProvider provider;

    public ResponseWrapperConverter(EntityFactory entityFactory, ObjectMapperProvider springDocObjectMapper) {
        super(springDocObjectMapper);
        this.entityFactory = entityFactory;
        this.provider = springDocObjectMapper;
    }

    @Override
    public Schema<?> resolve(AnnotatedType _type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = provider.jsonMapper().constructType(_type.getType());
        if (javaType != null) {
            _type.type(provider
                           .jsonMapper()
                           .constructType(getRealType(ResolvableType.forType(_type.getType())).getType()));
        }
        return super.resolve(_type, context, chain);
    }

    @Override
    public Operation customize(Operation operation, Components components, HandlerMethod handlerMethod) {
        MethodParameter parameter = handlerMethod.getReturnType();
        ApiResponses responses = operation.getResponses();

        if (responses != null) {
            // 只展示2xx的响应
            responses
                .keySet()
                .stream()
                .filter(code -> !code.startsWith("2"))
                .collect(Collectors.toSet())
                .forEach(operation.getResponses()::remove);

            // 原始返回类型
            ResolvableType originType = ResolvableType.forMethodParameter(
                handlerMethod.getReturnType(),
                ResolvableType.forType(handlerMethod.getBeanType()));

            for (Map.Entry<String, ApiResponse> entry : responses.entrySet()) {
                if (entry.getValue().getContent() == null) {
                    continue;
                }

                for (Map.Entry<String, MediaType> typeEntry :
                    entry.getValue().getContent().entrySet()) {

                    Schema<?> schema = typeEntry.getValue().getSchema();
                    if (schema != null) {
                        ResolvableType type = resolveWrappedType(originType);
                        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.parseMediaType(typeEntry.getKey());
                        // 流式响应,不包装.
                        if (org.springframework.http.MediaType.TEXT_EVENT_STREAM.includes(mediaType)
                            || org.springframework.http.MediaType.APPLICATION_NDJSON.includes(mediaType)) {
                            type = type.getGeneric(0);
                        }
                        if (originType.equalsType(type)) {
                            continue;
                        }
                        Type _type = provider
                            .jsonMapper()
                            .getTypeFactory()
                            .constructType(type.getType());

                        Schema<?> schema_ = SpringDocAnnotationsUtils
                            .extractSchema(components,
                                           _type,
                                           null,
                                           parameter.getParameterAnnotations(),
                                           SpecVersion.V31
                            );

                        typeEntry.getValue().schema(schema_);
                    }
                }
            }
        }

        return operation;
    }


    public Type resolveWrappedType(Type type) {
        return resolveWrappedType(ResolvableType.forType(type)).getType();
    }

    public ResolvableType resolveWrappedType(ResolvableType type) {
        Class<?> typeClass = type.toClass();

        @SuppressWarnings("all")
        Class<ResponseMessage> msgType = entityFactory.getInstanceType(ResponseMessage.class);

        // 处理响应式类型
        if (Publisher.class.isAssignableFrom(typeClass)) {
            ResolvableType actualType = type.getGeneric(0);
            // 如果已经是ResponseEntity或者ResponseMessage
            if (ResponseEntity.class.isAssignableFrom(actualType.toClass()) ||
                ResponseMessage.class.isAssignableFrom(actualType.toClass())) {
                ResolvableType real = getRealType(actualType.getGeneric(0));
                return ResolvableType
                    .forClassWithGenerics(actualType.toClass(), real);
            }
            ResolvableType realType = getRealType(actualType);
            // flux 返回List
            if (Flux.class.isAssignableFrom(type.toClass())) {
                return ResolvableType
                    .forClassWithGenerics(
                        msgType,
                        ResolvableType.forClassWithGenerics(List.class, realType)
                    );
            }

            return ResolvableType
                .forClassWithGenerics(
                    msgType,
                    realType
                );
        }

        // 处理ResponseEntity 或者 ResponseMessage
        if (ResponseEntity.class.isAssignableFrom(typeClass)
            || ResponseMessage.class.isAssignableFrom(typeClass)) {
            ResolvableType realType = getRealType(type.getGeneric(0));
            return ResolvableType
                .forClassWithGenerics(type.toClass(), realType);
        }

        // 其他类型，直接获取真实类型并包装到ResponseMessage中
        ResolvableType realType = getRealType(type);

        return ResolvableType.forClassWithGenerics(msgType, realType);
    }

    private ResolvableType getRealType(ResolvableType type) {
        if (isIgnoreWrapped(type.getType())) {
            return type;
        }

        Class<?> typeClazz = type.toClass();
        // Iterable
        if (Iterable.class.isAssignableFrom(typeClazz)) {
            ResolvableType _type = ResolvableType
                .forType(typeClazz)
                .as(Iterable.class);
            return ResolvableType.forClassWithGenerics(
                Iterable.class, getRealType(_type.getGeneric(0))
            );
        }
        // Map<?,?>
        if (Map.class.isAssignableFrom(typeClazz)) {
            ResolvableType _type = ResolvableType
                .forType(typeClazz)
                .as(Map.class);
            return ResolvableType.forClassWithGenerics(
                Map.class,
                _type.getGeneric(0),
                getRealType(_type.getGeneric(1))
            );
        }
        if (typeClazz != Object.class) {
            Class<?> t = entityFactory.getInstanceType(typeClazz);
            if (t == null) {
                return type;
            }
            ResolvableType resolved = ResolvableType.forClass(t);
            ResolvableType[] generics = resolved.getGenerics();
            ResolvableType[] typeGenerics = type.getGenerics();
            // 泛型
            // todo 不同长度的匹配?
            if (generics.length > 0 && generics.length == typeGenerics.length) {
                return ResolvableType.forClassWithGenerics(
                    t,
                    Arrays.stream(typeGenerics)
                          .map(this::getRealType)
                          .toArray(ResolvableType[]::new)
                );
            }

            return ResolvableType.forClass(t);
        }

        // 如果不是Class类型，则直接返回原类型
        return type;
    }

    private boolean isIgnoreWrapped(Type type) {
        if (type == null) {
            return true;
        }
        if (type instanceof Class<?> clazz) {
            return clazz == Object.class
                || ClassUtils.isPrimitiveOrWrapper(clazz)
                || CharSequence.class.isAssignableFrom(clazz)
                || Enum.class.isAssignableFrom(clazz);
        }
        return false;
    }


    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {

        return operation;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}

