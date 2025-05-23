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
package org.jetlinks.community.rule.engine.commons;

import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposables;
import reactor.core.Scannable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ShakeLimitFlux<T> extends Flux<ShakeLimitResult<T>> implements Scannable {
    private final String key;
    private final Flux<T> source;
    private final ShakeLimit limit;
    private final Publisher<?> resetSignal;

    private ShakeLimitFlux(String key, Flux<T> source, ShakeLimit limit, Publisher<?> resetSignal) {
        this.key = key;
        this.source = source;
        this.limit = limit;
        this.resetSignal = resetSignal;
    }

    public static <T> ShakeLimitFlux<T> create(String key,
                                               Flux<T> source,
                                               ShakeLimit limit) {
        return new ShakeLimitFlux<>(key, source, limit, Mono.never());
    }

    public static <T> ShakeLimitFlux<T> create(String key,
                                               Flux<T> source,
                                               ShakeLimit limit,
                                               Publisher<?> resetSignal) {
        return new ShakeLimitFlux<>(key, source, limit, resetSignal);
    }


    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super ShakeLimitResult<T>> actual) {
        ShakeLimitSubscriber<T> subscriber = new ShakeLimitSubscriber<>(key, actual, limit, resetSignal);
        source.subscribe(subscriber);
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr key) {
        if (key == Attr.RUN_STYLE) return Attr.RunStyle.ASYNC;
        if (key == Attr.PARENT) return source;
        if (key == Attr.NAME) return stepName();
        return null;
    }

    @Override
    @Nonnull
    public String stepName() {
        return "ShakeLimit('" + key + "','" + limit + "')";
    }

    @RequiredArgsConstructor
    static class ShakeLimitSubscriber<T> extends BaseSubscriber<T> implements Scannable, Runnable {
        final String key;
        final CoreSubscriber<? super ShakeLimitResult<T>> actual;
        final ShakeLimit limit;
        final Publisher<?> resetSignal;
        final Swap timer = Disposables.swap();

        static final int STATE_INIT = 0,
            STATE_PAUSED = 1;

        @SuppressWarnings("all")
        static final AtomicIntegerFieldUpdater<ShakeLimitSubscriber>
            STATE = AtomicIntegerFieldUpdater.newUpdater(ShakeLimitSubscriber.class, "state");
        volatile int state;

        ResetSubscriber resetSubscriber;
        @SuppressWarnings("all")
        static final AtomicLongFieldUpdater<ShakeLimitSubscriber>
            COUNT = AtomicLongFieldUpdater.newUpdater(ShakeLimitSubscriber.class, "count");
        volatile long count;

        @SuppressWarnings("all")
        static final AtomicReferenceFieldUpdater<ShakeLimitSubscriber, Object> FIRST =
            AtomicReferenceFieldUpdater.newUpdater(ShakeLimitSubscriber.class, Object.class, "first");
        volatile T first;

        @SuppressWarnings("all")
        static final AtomicLongFieldUpdater<ShakeLimitSubscriber>
            FIRST_TIME = AtomicLongFieldUpdater.newUpdater(ShakeLimitSubscriber.class, "firstTime");
        volatile long firstTime;

        @SuppressWarnings("all")
        static final AtomicReferenceFieldUpdater<ShakeLimitSubscriber, Object> LAST =
            AtomicReferenceFieldUpdater.newUpdater(ShakeLimitSubscriber.class, Object.class, "last");
        volatile T last;

        @SuppressWarnings("all")
        static final AtomicLongFieldUpdater<ShakeLimitSubscriber>
            LAST_TIME = AtomicLongFieldUpdater.newUpdater(ShakeLimitSubscriber.class, "lastTime");
        volatile long lastTime;

        @Override
        public void run() {
            long count = COUNT.getAndSet(this, 0);
            //尝试触发
            if (STATE.getAndSet(this, STATE_INIT) != STATE_PAUSED) {
                handle(count, true);
            }
        }

        class ResetSubscriber extends BaseSubscriber<Object> {

            @Override
            protected void hookOnNext(@Nonnull Object value) {
                //重置
                reset(true);

            }

            @Override
            @Nonnull
            public Context currentContext() {
                return actual.currentContext();
            }
        }

        private void reset(boolean force) {
            if (force) {
                STATE.set(this, STATE_INIT);
                FIRST_TIME.set(this, 0);
            }
            COUNT.set(this, 0);
            FIRST.set(this, null);
            LAST.set(this, null);
            LAST_TIME.set(this, 0);
            //重置定时
            completeTimer();
        }

        @Override
        @Nonnull
        public Context currentContext() {
            return actual.currentContext();
        }

        @Override
        protected void hookOnSubscribe(@Nonnull Subscription subscription) {
            //连续触发的场景,需要根据重置信号进行重置
            if (resetSignal != null && limit.isContinuous()) {
                resetSubscriber = new ResetSubscriber();
                resetSignal.subscribe(resetSubscriber);
            }

            actual.onSubscribe(this);
            request(1);

        }

        @Override
        protected void hookOnNext(@Nonnull T value) {
            long now = System.currentTimeMillis();

            startTimer();

//            long firstTime = FIRST_TIME.get(this);
////
//            //定时重置未及时生效?
//            if (limit.isRolling()
//                && firstTime > 0 && limit.getTime() > 0
//                //跨越了新的时间窗口
//                && now - firstTime > limit.getTime() * 1000L) {
//                //重置,重新开始计数
//                handle(COUNT.getAndSet(this,0),true);
//            }

            FIRST.compareAndSet(this, null, value);
            FIRST_TIME.compareAndSet(this, 0, now);

            LAST.set(this, value);
            LAST_TIME.set(this, now);

            long count = COUNT.incrementAndGet(this);
            //尝试立即触发
            if (limit.isAlarmFirst()) {
                //滚动窗口,或者不按时间窗口,直接处理
                if (limit.isRolling() || limit.getTime() <= 0) {
                    handle(count, false);
                } else {
                    STATE.accumulateAndGet(
                        this, STATE_PAUSED,
                        (old, update) -> {
                            if (old != STATE_PAUSED) {
                                if (handle(count, false)) {
                                    return STATE_PAUSED;
                                }
                            }
                            return old;
                        });
                }
            }
            request(1);
        }


        @Override
        protected void hookOnError(@Nonnull Throwable throwable) {
            actual.onError(throwable);
        }

        @Override
        protected void hookOnComplete() {
            try {
                if (STATE.get(this) != STATE_PAUSED) {
                    long now = System.currentTimeMillis();

                    long count = COUNT.getAndSet(this, 0);
                    if (count < limit.getThreshold()) {
                        return;
                    }
                    //没在窗口内
                    if (limit.getTime() > 0 && now - firstTime <= limit.getTime() * 1000L) {
                        return;
                    }
                    handle(count, true);
                }
            } finally {
                actual.onComplete();
            }

        }

        @Override
        protected void hookFinally(@Nonnull SignalType type) {
            timer.dispose();
            if (resetSubscriber != null) {
                resetSubscriber.cancel();
            }
        }

        protected void completeTimer() {
            //滚动窗口才重置
            if (limit.isRolling()) {
                timer.update(Disposables.disposed());
                startTimer();
            }
        }

        protected void startTimer() {
            if (limit.getTime() <= 0 || isDisposed()) {
                return;
            }

            if (timer.get() == null || timer.get().isDisposed()) {
                synchronized (this) {
                    if (timer.get() == null || timer.get().isDisposed()) {
                        if (limit.isRolling()) {
                            timer.update(
                                Schedulers
                                    .parallel()
                                    .schedule(this,
                                              limit.getTime(),
                                              TimeUnit.SECONDS)
                            );
                        } else {
                            timer.update(
                                Schedulers
                                    .parallel()
                                    .schedulePeriodically(
                                        this,
                                        limit.getTime(),
                                        limit.getTime(),
                                        TimeUnit.SECONDS)
                            );
                        }

                    }
                }
            }

        }

        protected boolean handle(long count, boolean reset) {
            //未满足条件
            if (count < limit.getThreshold()) {
                if (reset) {
                    reset(true);
                }
                return false;
            }
            //take first or last
            T val = limit.isOutputFirst() ? first : last;
            reset(reset);
            if (val != null) {
                actual.onNext(new ShakeLimitResult<>(key, count, val));
                return true;
            }
            return false;
        }

        @Override
        public Object scanUnsafe(@Nonnull Attr key) {
            if (key == Attr.PREFETCH) return 1;
            if (key == Attr.ACTUAL) return actual;
            if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;

            return null;
        }
    }
}
