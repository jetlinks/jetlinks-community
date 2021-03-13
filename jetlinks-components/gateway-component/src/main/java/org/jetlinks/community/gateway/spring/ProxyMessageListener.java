package org.jetlinks.community.gateway.spring;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.proxy.Proxy;
import org.jetlinks.core.NativePayload;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.Decoder;
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

    private volatile Decoder<?> decoder;

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
        this.resolvableType = ResolvableType.forMethodParameter(method, 0, targetType);
        this.proxy = Proxy.create(BiFunction.class)
            .addMethod(code.toString())
            .newInstance();

    }

    Object convert(TopicPayload message) {

        if (Payload.class.isAssignableFrom(paramType)) {
            return message;
        }
        try {
            Payload payload = message.getPayload();
            Object decodedPayload;
            if (payload instanceof NativePayload) {
                decodedPayload = ((NativePayload<?>) payload).getNativeObject();
            } else {
                if (decoder == null) {
                    decoder = Codecs.lookup(resolvableType);
                }
                decodedPayload = decoder.decode(message);
            }
            if (paramType.isInstance(decodedPayload)) {
                return decodedPayload;
            }
            return FastBeanCopier.DEFAULT_CONVERT.convert(decodedPayload, paramType, resolvableType.resolveGenerics());
        } finally {
            message.release();
        }
    }

    @Override
    public Mono<Void> onMessage(TopicPayload message) {
        try {
            boolean paramVoid = paramType == Void.class;
            try {
                Object val = proxy.apply(target, paramVoid ? null : convert(message));
                if (val instanceof Publisher) {
                    return Mono.from((Publisher<?>) val).then();
                }
                return Mono.empty();
            } finally {
                if (paramVoid) {
                    message.release();
                }
            }
        } catch (Throwable e) {
            log.error("invoke event listener [{}] error", toString(), e);
        }
        return Mono.empty();
    }

    @Override
    public String toString() {
        return ClassUtils.getUserClass(target).getSimpleName() + "." + method.getName();
    }
}
