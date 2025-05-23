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
import org.hswebframework.web.bean.FastBeanCopier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class CompositeDeviceSelectorProvider implements DeviceSelectorProvider {

    public static final String PROVIDER = "composite";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getName() {
        return "组合选择";
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(List<?> args,
                                                                              NestConditional<T> conditional) {
        //暂不支持
        return Mono.just(conditional);
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(DeviceSelectorSpec source,
                                                                              Map<String, Object> ctx,
                                                                              NestConditional<T> conditional) {
        Mono<NestConditional<T>> handler = null;
        for (SelectorValue selectorValue : source.getSelectorValues()) {
            DeviceSelectorSpec spec = FastBeanCopier.copy(selectorValue.getValue(), new DeviceSelectorSpec());
            if (handler == null) {
                handler = DeviceSelectorProviders
                    .getProviderNow(spec.getSelector())
                    .applyCondition(spec, ctx, conditional);
            } else {
                handler = handler
                    .flatMap(ctd -> DeviceSelectorProviders
                        .getProviderNow(spec.getSelector())
                        .applyCondition(spec, ctx, ctd));
            }
        }
        return handler == null ? Mono.just(conditional) : handler;
    }

    @Override
    public <T extends Conditional<T>> BiFunction<
        NestConditional<T>,
        Map<String, Object>,
        Mono<NestConditional<T>>> createLazy(DeviceSelectorSpec source) {

        BiFunction<NestConditional<T>, Map<String, Object>, Mono<NestConditional<T>>> function = null;

        for (SelectorValue selectorValue : source.getSelectorValues()) {
            DeviceSelectorSpec spec = FastBeanCopier.copy(selectorValue.getValue(), new DeviceSelectorSpec());
            DeviceSelectorProvider provider = DeviceSelectorProviders.getProviderNow(spec.getSelector());

            BiFunction<NestConditional<T>, Map<String, Object>, Mono<NestConditional<T>>> that = provider.createLazy(spec);
            if (function == null) {
                function = that;
            } else {
                BiFunction<NestConditional<T>, Map<String, Object>, Mono<NestConditional<T>>> temp = function;

                function = (condition, ctx) -> temp.apply(condition, ctx).flatMap(ctd -> that.apply(condition, ctx));
            }
        }
        return function == null ? (condition, ignore) -> Mono.just(condition) : function;
    }
}
