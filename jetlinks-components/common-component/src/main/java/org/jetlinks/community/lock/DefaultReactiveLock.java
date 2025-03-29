package org.jetlinks.community.lock;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

class DefaultReactiveLock implements ReactiveLock {
    @SuppressWarnings("all")
    static final AtomicReferenceFieldUpdater<DefaultReactiveLock, LockingSubscriber>
        PENDING = AtomicReferenceFieldUpdater
        .newUpdater(DefaultReactiveLock.class, LockingSubscriber.class, "pending");

    final Deque<LockingSubscriber<?>> queue = new ConcurrentLinkedDeque<>();

    volatile LockingSubscriber<?> pending;


    @Override
    public <T> Flux<T> lock(Flux<T> job) {
        return new LockingFlux<>(this, job);
    }

    @Override
    public <T> Flux<T> lock(Flux<T> flux, Duration timeout) {
        return new LockingFlux<>(this, flux, timeout);
    }

    @Override
    public <T> Flux<T> lock(Flux<T> flux, Duration timeout, Flux<? extends T> fallback) {
        return new LockingFlux<>(this, flux, timeout, fallback);
    }

    @Override
    public <T> Mono<T> lock(Mono<T> job) {
        return new LockingMono<>(this, job);
    }

    @Override
    public <T> Mono<T> lock(Mono<T> mono, Duration timeout) {
        return new LockingMono<>(this, mono, timeout);
    }

    @Override
    public <T> Mono<T> lock(Mono<T> mono, Duration timeout, Mono<? extends T> fallback) {
        return new LockingMono<>(this, mono, timeout, fallback);
    }

    protected void drain() {
        if (PENDING.get(this) != null) {
            return;
        }

        LockingSubscriber<?> locking;

        for (; ; ) {
            locking = queue.pollFirst();
            if (locking == null) {
                return;
            }
            if (locking.isDisposed()) {
                continue;
            }
            if (PENDING.compareAndSet(this, null, locking)) {
                //使用单独的线程池来调度,防止参与锁太多导致栈溢出.
                Schedulers.parallel().schedule(locking::subscribe);
            } else {
                queue.addLast(locking);
            }
            break;
        }

    }


    <T> void registerSubscriber(CoreSubscriber<? super T> actual,
                                Consumer<CoreSubscriber<? super T>> subscribeCallback,
                                @Nullable Duration timeout,
                                @Nullable Publisher<? extends T> timeoutFallback) {
        registerSubscriber(new LockingSubscriber<>(
            this,
            actual,
            subscribeCallback,
            timeout,
            timeoutFallback));
    }

    void registerSubscriber(LockingSubscriber<?> subscriber) {
        if (PENDING.compareAndSet(this, null, subscriber)) {
            subscriber.subscribe();
            return;
        }

        queue.addLast(subscriber);
        drain();
    }

    static class LockingFlux<T> extends FluxOperator<T, T> {
        private final DefaultReactiveLock main;

        private Duration timeout;

        private Publisher<? extends T> timeoutFallback;

        protected LockingFlux(DefaultReactiveLock main, Flux<? extends T> source) {
            super(source);
            this.main = main;
        }

        protected LockingFlux(DefaultReactiveLock main, Flux<? extends T> source, Duration timeout) {
            super(source);
            this.main = main;
            this.timeout = timeout;
        }

        protected LockingFlux(DefaultReactiveLock main, Flux<? extends T> source, Duration timeout, Flux<? extends T> timeoutFallback) {
            super(source);
            this.main = main;
            this.timeout = timeout;
            this.timeoutFallback = timeoutFallback;
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            Consumer<CoreSubscriber<? super T>> subscribeCallback = source::subscribe;
            main.registerSubscriber(actual, subscribeCallback, timeout, timeoutFallback);
        }

    }

    static class LockingMono<T> extends MonoOperator<T, T> {
        private final DefaultReactiveLock main;
        private Duration timeout;

        private Publisher<? extends T> fallback;

        protected LockingMono(DefaultReactiveLock main, Mono<? extends T> source) {
            super(source);
            this.main = main;
        }

        protected LockingMono(DefaultReactiveLock main, Mono<? extends T> source, Duration timeout) {
            super(source);
            this.main = main;
            this.timeout = timeout;
        }

        protected LockingMono(DefaultReactiveLock main, Mono<? extends T> source, Duration timeout, Mono<? extends T> fallback) {
            super(source);
            this.main = main;
            this.timeout = timeout;
            this.fallback = fallback;
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            Consumer<CoreSubscriber<? super T>> subscribeCallback = source::subscribe;
            main.registerSubscriber(actual, subscribeCallback, timeout, fallback);
        }


    }

    static class LockingSubscriber<T> extends BaseSubscriber<T> {
        protected final DefaultReactiveLock main;
        protected final CoreSubscriber<? super T> actual;
        private final Consumer<CoreSubscriber<? super T>> subscriber;
        private Disposable timeoutTask;
        private final Publisher<? extends T> timeoutFallback;
        @SuppressWarnings("all")
        protected static final AtomicIntegerFieldUpdater<LockingSubscriber> statusUpdater =
            AtomicIntegerFieldUpdater.newUpdater(LockingSubscriber.class, "status");

        private volatile int status;

        //初始
        private static final int INIT = 0;

        //订阅备用流
        private static final int SUB_TIMEOUT_FALLBACK = -1;

        //订阅原本上游流
        private static final int SUB_SOURCE = 1;

        //流结束
        private static final int UN_SUB = -2;

        public LockingSubscriber(DefaultReactiveLock main,
                                 CoreSubscriber<? super T> actual,
                                 Consumer<CoreSubscriber<? super T>> subscriber,
                                 @Nullable Duration timeout,
                                 @Nullable Publisher<? extends T> timeoutFallback) {
            this.actual = actual;
            this.main = main;
            this.subscriber = subscriber;
            this.timeoutFallback = timeoutFallback;
            if (timeout != null) {
                this.timeoutTask = Schedulers
                    .parallel()
                    .schedule(this::onTimeout, timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

        }

        private void onTimeout() {
            if (statusUpdater.compareAndSet(this, INIT, SUB_TIMEOUT_FALLBACK)) {
                //不代理订阅，直接取消流及释放当前锁，以免并发时等待备用流释放锁
                doComplete();
                if (timeoutFallback != null) {
                    timeoutFallback.subscribe(actual);
                } else {
                    this.onError(new TimeoutException("Lock timed out"));
                }
            }
        }

        protected void subscribe() {
            if (statusUpdater.compareAndSet(this, INIT, SUB_SOURCE)) {
                if (timeoutTask != null && !timeoutTask.isDisposed()) {
                    timeoutTask.dispose();
                }
                subscriber.accept(this);
            }
        }

        protected void complete() {
            if (statusUpdater.compareAndSet(this, INIT, UN_SUB) || statusUpdater.compareAndSet(this, SUB_SOURCE, UN_SUB)) {
                if (timeoutTask != null && !timeoutTask.isDisposed()) {
                    timeoutTask.dispose();
                }
                doComplete();
            }
        }

        protected void doComplete() {
            //防止非hookFinally触发的结束
            if (!this.isDisposed()) {
                this.cancel();
            }
            if (PENDING.compareAndSet(main, this, null)) {
                main.drain();
            }
        }

        @Override
        protected final void hookOnError(@Nonnull Throwable throwable) {
            actual.onError(throwable);
        }

        @Override
        protected final void hookOnNext(@Nonnull T value) {
            actual.onNext(value);
        }

        @Override
        protected final void hookOnSubscribe(@Nonnull Subscription subscription) {
            actual.onSubscribe(this);
        }

        @Override
        protected final void hookOnComplete() {
            actual.onComplete();
        }

        @Override
        protected final void hookOnCancel() {
            super.hookOnCancel();
        }

        @Override
        protected final void hookFinally(@Nonnull SignalType type) {
            complete();
        }

        @Override
        @Nonnull
        public Context currentContext() {
            return actual.currentContext();
        }
    }

}
