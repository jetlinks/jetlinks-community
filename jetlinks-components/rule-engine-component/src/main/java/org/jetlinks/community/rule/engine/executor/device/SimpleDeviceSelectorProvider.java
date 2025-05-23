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
package org.jetlinks.community.rule.engine.executor.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.NestConditional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Getter
@AllArgsConstructor
@SuppressWarnings("all")
public class SimpleDeviceSelectorProvider implements DeviceSelectorProvider {
    private final String provider;
    private final String name;

    private final BiFunction<List<?>, NestConditional<?>, Mono<NestConditional<?>>> function;

    public static SimpleDeviceSelectorProvider of(
        String provider,
        String name,
        BiFunction<List<?>, NestConditional<?>, NestConditional<?>> function) {
        return new SimpleDeviceSelectorProvider(provider, name, (args, condition) -> {
            return Mono.just(function.apply(args, condition));
        });
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(List<?> args,
                                                                              NestConditional<T> conditional) {
        return (Mono) function.apply(args, conditional).defaultIfEmpty(conditional);
    }


    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(DeviceSelectorSpec source,
                                                                              Map<String, Object> ctx,
                                                                              NestConditional<T> conditional) {

        return source
            .resolveSelectorValues(ctx)
            .collectList()
            .flatMap(list -> applyCondition(list, conditional));
    }
}
