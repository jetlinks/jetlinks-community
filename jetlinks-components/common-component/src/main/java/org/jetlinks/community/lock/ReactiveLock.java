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
