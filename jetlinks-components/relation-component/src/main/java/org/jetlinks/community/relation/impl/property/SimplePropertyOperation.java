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
