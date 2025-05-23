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
package org.jetlinks.community.gateway.spring;

import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.proxy.Proxy;
import org.jetlinks.core.event.TopicPayload;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.StringJoiner;
import java.util.function.BiFunction;

@Slf4j
class ProxyMessageListener implements MessageListener {
    private final Class<?> paramType;
    private final Object target;
    private final ResolvableType resolvableType;

    private final Method method;
    private final BiFunction<Object, Object, Object> proxy;

    private final boolean paramIsPayload;

    private final boolean paramIsVoid;


    @SuppressWarnings("all")
    ProxyMessageListener(Object target, Method method) {
        this.target = target;
        this.method = method;
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 1) {
            throw new UnsupportedOperationException("unsupported method [" + method + "] parameter");
        }
        if (parameterTypes.length == 1) {
            paramType = parameterTypes[0];
        } else {
            paramType = Void.class;
        }

        Class<?> targetType = ClassUtils.getUserClass(target);

        StringJoiner code = new StringJoiner("\n");

        code.add("public Object apply(Object target,Object param){");
        code.add(targetType.getName() + " _target = (" + targetType.getName() + ")target;");

        String invokeCode;
        if (paramType != Void.class) {
            if (paramType.isPrimitive()) {
                throw new UnsupportedOperationException(method + "参数不支持基本数据类型,请使用包装器类型.");
            }
            code.add(paramType.getName() + " _param = (" + paramType.getName() + ")param;");
            invokeCode = " _target." + method.getName() + "(_param);";
        } else {
            invokeCode = " _target." + method.getName() + "();";
        }
        if (method.getReturnType() != Void.TYPE) {
            code.add("return " + invokeCode);
        } else {
            code.add(invokeCode)
                .add("return null;");
        }

        code.add("}");
        if (paramType == Void.class) {
            this.resolvableType = ResolvableType.forClass(Void.class);
        } else {
            this.resolvableType = ResolvableType.forMethodParameter(method, 0, targetType);
        }
        this.proxy = Proxy.create(BiFunction.class)
                          .addMethod(code.toString())
                          .newInstance();

        paramIsPayload = TopicPayload.class.isAssignableFrom(paramType);
        paramIsVoid = paramType == Void.class;
    }

    Object convert(TopicPayload message) {
        if (paramIsPayload) {
            return message;
        }
        try {
            Object decodedPayload = message.decode(false);
            if (paramType.isInstance(decodedPayload)) {
                return decodedPayload;
            }
            return FastBeanCopier.DEFAULT_CONVERT.convert(decodedPayload, paramType, resolvableType.resolveGenerics());
        } finally {
            ReferenceCountUtil.safeRelease(message);
        }
    }

    @Override
    public Mono<Void> onMessage(TopicPayload message) {
        try {
            Object val = proxy.apply(target, paramIsVoid ? null : convert(message));
            if (val instanceof Publisher) {
                return Mono.from((Publisher<?>) val).then();
            }
            return Mono.empty();
        } catch (Throwable e) {
            return Mono.error(e);
        }
    }

    @Override
    public String toString() {
        return ClassUtils.getUserClass(target).getSimpleName() + "." + method.getName();
    }
}
