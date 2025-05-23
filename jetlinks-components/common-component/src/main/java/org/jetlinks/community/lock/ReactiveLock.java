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
package org.jetlinks.community.lock;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 响应式锁
 *
 * @author zhouhao
 * @since 2.2
 */
public interface ReactiveLock {

    /**
     * 对Mono进行加锁,将等待之前的lock完成后再执行.
     *
     * @param mono 要加锁的Mono
     * @param <T>  T
     * @return 加锁后的Mono
     */
    <T> Mono<T> lock(Mono<T> mono);

    /**
     * 对Mono进行加锁,将等待之前的lock完成后再执行,若等待锁时间超过{@link Duration}，则报错{@link java.util.concurrent.TimeoutException}
     *
     * @param mono    要加锁的Mono
     * @param timeout 等待锁的时间
     * @param <T>     T
     * @return 加锁后的Mono
     */
    <T> Mono<T> lock(Mono<T> mono, Duration timeout);

    /**
     * 对Mono进行加锁,将等待之前的lock完成后再执行,若等待锁时间超过{@link Duration}，则切换到回退流
     *
     * @param mono     要加锁的Mono
     * @param timeout  等待锁的时间
     * @param fallback 发生超时时要订阅的回退流
     * @param <T>      T
     * @return 加锁后的Mono
     */
    <T> Mono<T> lock(Mono<T> mono, Duration timeout, Mono<? extends T> fallback);

    /**
     * 对Flux进行加锁,将等待之前的lock完成后再执行.
     *
     * @param flux 要加锁的Flux
     * @param <T>  T
     * @return 加锁后的Flux
     */
    <T> Flux<T> lock(Flux<T> flux);

    /**
     * 对Flux进行加锁,将等待之前的lock完成后再执行,若等待锁时间超过{@link Duration}，则报错{@link java.util.concurrent.TimeoutException}
     *
     * @param flux    要加锁的Flux
     * @param timeout 等待锁的时间
     * @param <T>     T
     * @return 加锁后的Mono
     */
    <T> Flux<T> lock(Flux<T> flux, Duration timeout);

    /**
     * 对Flux进行加锁,将等待之前的lock完成后再执行,若等待锁时间超过{@link Duration}，则切换到回退流
     *
     * @param flux     要加锁的Flux
     * @param timeout  等待锁的时间
     * @param fallback 发生超时时要订阅的回退流
     * @param <T>      T
     * @return 加锁后的Mono
     */
    <T> Flux<T> lock(Flux<T> flux, Duration timeout, Flux<? extends T> fallback);

}
