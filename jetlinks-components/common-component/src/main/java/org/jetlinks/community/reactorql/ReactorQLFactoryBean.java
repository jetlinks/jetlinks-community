package org.jetlinks.community.reactorql;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ReactorQLFactoryBean implements FactoryBean<Object>, InitializingBean, Ordered {

    @Getter
    @Setter
    private Class<?> target;

    private Object proxy;

    private static final ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    public ReactorQLFactoryBean() {

    }

    @Override
    public Object getObject() {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return target;
    }

    @Override
    public void afterPropertiesSet() {
        Map<Method, Function<Object[], Object>> cache = new ConcurrentHashMap<>();

        this.proxy = Proxy
            .newProxyInstance(ClassUtils.getDefaultClassLoader(),
                              new Class[]{target},
                              (proxy, method, args) ->
                                  cache
                                      .computeIfAbsent(method, mtd -> createInvoker(target, mtd, mtd.getAnnotation(ReactorQL.class)))
                                      .apply(args));
    }

    @SneakyThrows
    private Function<Object[], Object> createInvoker(Class<?> type, Method method, ReactorQL ql) {
        if (method.isDefault() || ql == null) {
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                .getDeclaredConstructor(Class.class);
            constructor.setAccessible(true);
            MethodHandles.Lookup lookup = constructor.newInstance(type);
            MethodHandle handle = lookup
                .in(type)
                .unreflectSpecial(method, type)
                .bindTo(proxy);
            return args -> {
                try {
                    return handle.invokeWithArguments(args);
                } catch (Throwable e) {
                    return Mono.error(e);
                }
            };
        }

        ResolvableType returnType = ResolvableType.forMethodReturnType(method);
        if (returnType.toClass() != Mono.class && returnType.toClass() != Flux.class) {
            throw new UnsupportedOperationException("方法返回值必须为Mono或者Flux");
        }
        Class<?> genericType = returnType.getGeneric(0).toClass();
        Function<Map<String, Object>, ?> mapper;

        if (genericType == Map.class || genericType == Object.class) {
            mapper = Function.identity();
        } else {
            mapper = map -> FastBeanCopier.copy(map, genericType);
        }

        Function<Flux<?>, Publisher<?>> resultMapper =
            returnType.resolve() == Mono.class
                ? flux -> flux.take(1).singleOrEmpty()
                : flux -> flux;

        String[] names = nameDiscoverer.getParameterNames(method);

        try {
            org.jetlinks.reactor.ql.ReactorQL reactorQL =
                org.jetlinks.reactor.ql.ReactorQL
                    .builder()
                    .sql(ql.value())
                    .build();

            return args -> {
                Map<String, Object> argsMap = new HashMap<>();
                ReactorQLContext context = ReactorQLContext.ofDatasource(name -> {
                    if (args.length == 0) {
                        return Flux.just(1);
                    }
                    if (args.length == 1) {
                        return convertToFlux(args[0]);
                    }
                    return convertToFlux(argsMap.get(name));
                });
                for (int i = 0; i < args.length; i++) {
                    String indexName = "arg" + i;

                    String name = names == null ? indexName : names[i];
                    context.bind(i, args[i]);
                    context.bind(name, args[i]);
                    context.bind(indexName, args[i]);
                    argsMap.put(names == null ? indexName : names[i], args[i]);
                    argsMap.put(indexName, args[i]);
                }
                return reactorQL.start(context)
                                .map(record -> mapper.apply(record.asMap()))
                                .as(resultMapper);
            };
        } catch (Throwable e) {
            throw new IllegalArgumentException(
                "create ReactorQL method [" + method + "] error,sql:\n" + (String.join(" ", ql.value())), e);
        }
    }

    protected Flux<Object> convertToFlux(Object arg) {
        if (arg == null) {
            return Flux.empty();
        }
        if (arg instanceof Publisher) {
            return Flux.from((Publisher<?>) arg);
        }
        if (arg instanceof Iterable) {
            return Flux.fromIterable(((Iterable<?>) arg));
        }
        if (arg instanceof Object[]) {
            return Flux.fromArray(((Object[]) arg));
        }
        return Flux.just(arg);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
