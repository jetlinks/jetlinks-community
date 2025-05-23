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

import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.NestConditional;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface DeviceSelectorProvider extends Ordered {

    String getProvider();

    String getName();

    <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(List<?> args,
                                                                       NestConditional<T> conditional);

    <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(DeviceSelectorSpec source,
                                                                       Map<String,Object> ctx,
                                                                       NestConditional<T> conditional);


    default <T extends Conditional<T>> BiFunction<NestConditional<T>, Map<String, Object>, Mono<NestConditional<T>>> createLazy(
        DeviceSelectorSpec source) {
        return (condition, ctx) -> applyCondition(source, ctx, condition);
    }

    @Override
    default int getOrder() {
        return 0;
    }
}
