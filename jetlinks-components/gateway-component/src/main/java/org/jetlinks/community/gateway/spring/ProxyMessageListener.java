package org.jetlinks.community.gateway.spring;

import io.netty.buffer.ByteBuf;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.proxy.Proxy;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.community.gateway.EncodableMessage;
import org.jetlinks.community.gateway.TopicMessage;
import org.reactivestreams.Publisher;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.StringJoiner;
import java.util.function.BiFunction;

class ProxyMessageListener implements MessageListener {
    private final Class<?> paramType;
    private final Object target;

    BiFunction<Object, Object, Object> proxy;

    @SuppressWarnings("all")
    ProxyMessageListener(Object target, Method method) {
        this.target = target;
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

        this.proxy = Proxy.create(BiFunction.class)
            .addMethod(code.toString())
            .newInstance();
    }

    Object convert(TopicMessage message) {
        if (paramType.isAssignableFrom(TopicMessage.class)) {
            return message;
        }
        if (paramType.isAssignableFrom(EncodedMessage.class)) {
            return message.getMessage();
        }
        if (paramType.isAssignableFrom(ByteBuf.class)) {
            return message.getMessage().getPayload();
        }

        Object payload = message.convertMessage();
        if (paramType.isInstance(payload)) {
            return payload;
        }
        if (payload instanceof byte[]) {
            return payload;
        }
        return FastBeanCopier.DEFAULT_CONVERT.convert(payload, paramType, new Class[]{});

    }

    @Override
    public Mono<Void> onMessage(TopicMessage message) {
        return Mono.defer(() -> {
            Object val = proxy.apply(target, convert(message));
            if (val instanceof Publisher) {
                return Mono.from((Publisher<?>) val).then();
            }
            return Mono.empty();
        });
    }
}
