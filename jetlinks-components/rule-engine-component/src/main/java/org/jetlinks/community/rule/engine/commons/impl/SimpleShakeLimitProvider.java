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
package org.jetlinks.community.rule.engine.commons.impl;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.ShakeLimitFlux;
import org.jetlinks.community.rule.engine.commons.ShakeLimitProvider;
import org.jetlinks.community.rule.engine.commons.ShakeLimitResult;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

@Slf4j
public class SimpleShakeLimitProvider implements ShakeLimitProvider {

    public static final ShakeLimitProvider GLOBAL = new SimpleShakeLimitProvider();

    public static final String PROVIDER = "simple";

    @Override
    public String provider() {
        return PROVIDER;
    }

    protected <T> Flux<T> wrapSource(String sourceKey, Flux<T> source) {
        return source;
    }

    @Override
    public <T> Flux<ShakeLimitResult<T>> shakeLimit(String sourceKey,
                                                    Flux<GroupedFlux<String, T>> grouped,
                                                    ShakeLimit limit,
                                                    Function<String, Publisher<?>> resetSignal) {
        Duration windowSpan = Duration.ofSeconds(limit.getTime());

        return grouped
            .flatMap(group -> {
                String groupKey = group.key();
                String key = sourceKey + ":" + groupKey;
                return Flux
                    .defer(() -> this
                        //使用timeout,当2倍窗口时间没有收到数据时,则结束分组.释放内存.
                        .wrapSource(key, group.timeout(windowSpan.plus(windowSpan), Mono.empty())))
                    .as(source -> this
                        .handleWindow(key,
                                      groupKey,
                                      limit,
                                      source,
                                      resetSignal.apply(groupKey)))
                    .onErrorResume(err -> {
                        log.warn("shake limit [{}] error", key, err);
                        return Mono.empty();
                    });
            }, Integer.MAX_VALUE);
    }


    protected <T> Flux<ShakeLimitResult<T>> handleWindow(String key,
                                                         String groupKey,
                                                         ShakeLimit limit,
                                                         Flux<T> source,
                                                         Publisher<?> resetSignal) {
        return ShakeLimitFlux.create(groupKey, source, limit, resetSignal);
    }
}
