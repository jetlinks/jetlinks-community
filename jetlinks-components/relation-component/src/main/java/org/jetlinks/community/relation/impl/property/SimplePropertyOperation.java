package org.jetlinks.community.relation.impl.property;

import org.jetlinks.core.config.ConfigKey;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SimplePropertyOperation<O> implements PropertyOperationStrategy {

    private final Map<String, Function<O, ?>> mappers = new HashMap<>();

    private final Mono<O> supplier;

    public SimplePropertyOperation(Mono<O> supplier) {
        this.supplier = supplier;
    }

    @Override
    public boolean isSupported(String key) {
        return mappers.containsKey(key);
    }

    public SimplePropertyOperation<O> addMapper(String key, Function<O, ?> mapper) {
        mappers.put(key, mapper);
        return this;
    }

    public <T> SimplePropertyOperation<O> addMapper(ConfigKey<T> key, Function<O, T> mapper) {
        return addMapper(key.getKey(), mapper);
    }

    public <T> SimplePropertyOperation<O> addAsyncMapper(ConfigKey<T> key, BiFunction<O, ConfigKey<T>, Mono<T>> mapper) {
        return addMapper(key.getKey(), (obj) -> mapper.apply(obj,key));
    }

    @Override
    public Mono<Object> get(String key) {
        Function<O, ?> mapper = mappers.get(key);
        if (mapper == null) {
            return Mono.empty();
        }
        return supplier
            .flatMap((obj) -> {
                Object val = mapper.apply(obj);
                if (val instanceof Publisher) {
                    return Mono.from(((Publisher<?>) val));
                }
                return Mono.justOrEmpty(val);
            });
    }

}
