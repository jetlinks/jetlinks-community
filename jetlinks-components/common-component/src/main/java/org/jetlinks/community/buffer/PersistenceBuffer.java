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
package org.jetlinks.community.buffer;

import com.google.common.collect.Collections2;
import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetlinks.core.cache.FileQueue;
import org.jetlinks.core.cache.FileQueueProxy;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.community.utils.FormatUtils;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 支持持久化的缓存批量操作工具,用于支持数据的批量操作,如批量写入数据到数据库等.
 * <p>
 * 数据将保存在一个文件队列里,如果写入速度跟不上,数据将会尝试写入到本地文件中.
 *
 * <pre>{@code
 *
 *    PersistenceBuffer<Data> writer = PersistenceBuffer
 *    .<Data>create(
 *       "./data/buffer", //文件目录
 *      "my-data.queue", //文件名
 *      Data::new,
 *      buffer->{
 *           // 返回false表示不重试
 *           return saveData(buffer);
 *      })
 *    .bufferSize(1000)//缓冲大小,当缓冲区超过此数量时将会立即执行写出操作.
 *    .bufferTimeout(Duration.ofSeconds(2))// 缓冲超时时间,当缓冲区超过此时间将会立即执行写出操作.
 *    .parallelism(2); //并行度,表示支持并行写入的线程数.
 *
 *    //开始批量写入数据
 *    writer.start();
 *
 *    //写入缓冲区
 *    writer.write(data);
 * }</pre>
 *
 * @param <T> 数据类型,需要实现Serializable接口
 * @author zhouhao
 * @since 2.0
 */
public class PersistenceBuffer<T extends Serializable> implements EvictionContext, Disposable {
    @SuppressWarnings("all")
    private final static AtomicIntegerFieldUpdater<PersistenceBuffer> WIP =
        AtomicIntegerFieldUpdater.newUpdater(PersistenceBuffer.class, "wip");

    @SuppressWarnings("all")
    private final static AtomicLongFieldUpdater<PersistenceBuffer> REMAINDER =
        AtomicLongFieldUpdater.newUpdater(PersistenceBuffer.class, "remainder");

    @SuppressWarnings("all")
    private final static AtomicLongFieldUpdater<PersistenceBuffer> DEAD_SZIE =
        AtomicLongFieldUpdater.newUpdater(PersistenceBuffer.class, "deadSize");

    @SuppressWarnings("all")
    private final static AtomicReferenceFieldUpdater<PersistenceBuffer, Collection> BUFFER =
        AtomicReferenceFieldUpdater.newUpdater(PersistenceBuffer.class, Collection.class, "buffer");

    @SuppressWarnings("all")
    private final static AtomicReferenceFieldUpdater<PersistenceBuffer, Boolean> DISPOSED =
        AtomicReferenceFieldUpdater.newUpdater(PersistenceBuffer.class, Boolean.class, "disposed");

    private Logger logger = LoggerFactory.getLogger(PersistenceBuffer.class);
    @Getter
    private String name = "unknown";

    //死队列,存储无法完成操作的数据
    private FileQueue<Buf<T>> queue;

    //死队列,存储无法完成操作的数据
    private FileQueue<Buf<T>> deadQueue;

    //缓冲数据处理器,实际处理缓冲数据的逻辑,比如写入数据库.
    private final BiFunction<Collection<Buffered<T>>, FlushContext<T>, Mono<Boolean>> handler;

    //缓冲区大小,超过此大小将执行 handler 处理逻辑
    private BufferSettings settings;
    //缓冲区
    private volatile Collection<Buf<T>> buffer;

    //反序列化时指定快速实例化
    private final Supplier<Externalizable> instanceBuilder;

    //上一次刷新时间
    private long lastFlushTime;

    //当前正在进行的操作
    volatile int wip;

    //剩余数量
    private volatile long remainder;

    //死数据数量
    private volatile long deadSize;

    //刷新缓冲区定时任务
    private Disposable intervalFlush;

    //独立的读写调度器
    private Scheduler writer, reader;

    private Throwable lastError;

    private volatile Boolean disposed = false;
    private boolean started = false;

    private final PersistenceBufferMBeanImpl<T> monitor = new PersistenceBufferMBeanImpl<>(this);

    public PersistenceBuffer(String filePath,
                             String fileName,
                             Supplier<T> newInstance,
                             Function<Flux<T>, Mono<Boolean>> handler) {
        this(BufferSettings.create(filePath, fileName), newInstance, handler);
    }

    public PersistenceBuffer(String filePath,
                             String fileName,
                             Function<Flux<T>, Mono<Boolean>> handler) {
        this(filePath, fileName, null, handler);
    }

    public PersistenceBuffer(BufferSettings settings,
                             Supplier<T> newInstance,
                             BiFunction<Collection<Buffered<T>>, FlushContext<T>, Mono<Boolean>> handler) {
        if (newInstance != null) {
            T data = newInstance.get();
            if (data instanceof Externalizable) {
                this.instanceBuilder = () -> (Externalizable) newInstance.get();
            } else {
                this.instanceBuilder = null;
            }
        } else {
            this.instanceBuilder = null;
        }
        this.settings = settings;
        this.handler = handler;
    }

    public PersistenceBuffer(BufferSettings settings,
                             Supplier<T> newInstance,
                             Function<Flux<T>, Mono<Boolean>> handler) {
        this(settings, newInstance, (list, ignore) -> handler.apply(Flux.fromIterable(list).map(Buffered::getData)));
    }

    public PersistenceBuffer<T> bufferSize(int size) {
        settings = settings.bufferSize(size);
        return this;
    }

    public PersistenceBuffer<T> bufferTimeout(Duration bufferTimeout) {
        settings = settings.bufferTimeout(bufferTimeout);
        return this;
    }

    public PersistenceBuffer<T> parallelism(int parallelism) {
        settings = settings.parallelism(parallelism);
        return this;
    }

    public PersistenceBuffer<T> maxRetry(int maxRetryTimes) {
        settings = settings.maxRetry(maxRetryTimes);
        return this;
    }

    public PersistenceBuffer<T> retryWhenError(Predicate<Throwable> predicate) {
        settings = settings.retryWhenError(predicate);
        return this;
    }

    public PersistenceBuffer<T> settings(Function<BufferSettings, BufferSettings> mapper) {
        settings = mapper.apply(settings);
        return this;
    }

    public PersistenceBuffer<T> name(String name) {
        this.name = name;
        logger = LoggerFactory.getLogger(PersistenceBuffer.class.getName() + "." + name);
        return this;
    }

    static <T> FileQueue<Buf<T>> wrap(FileQueue<Buf<T>> queue) {
        return new FileQueueProxy<Buf<T>>(queue) {
            @Override
            public void clear() {
                super.flush();
            }
        };
    }

    private static String getSafeFileName(String fileName) {
        return fileName.replaceAll("[\\s\\\\/:*?\"<>|]", "_");
    }

    public void init() {
        String filePath = settings.getFilePath();
        String fileName = getSafeFileName(settings.getFileName());
        Path path = Paths.get(filePath);

        BufDataType dataType = new BufDataType();

        //数据队列
        this.queue = wrap(FileQueue
                              .<Buf<T>>builder()
                              .name(fileName)
                              .path(path)
                              .option("valueType", dataType)
                              .option("concurrency", settings.getFileConcurrency())
                              .build());
        this.remainder = queue.size();
        //死队列,用于存放失败的数据
        this.deadQueue = wrap(FileQueue
                                  .<Buf<T>>builder()
                                  .name(fileName + ".dead")
                                  .path(path)
                                  .option("valueType", dataType)
                                  .build());
        this.deadSize = this.deadQueue.size();
        this.buffer = newBuffer();
        initScheduler();
        registerMbean();
    }

    private void initScheduler() {
        shutdownScheduler();
        this.writer = settings.getFileConcurrency() > 1
            ? Schedulers.newParallel(name + "-writer", settings.getFileConcurrency())
            : Schedulers.newSingle(name + "-writer");

        this.reader = Schedulers.newSingle(name + "-reader");
    }

    private void shutdownScheduler() {
        if (this.writer != null) {
            this.writer.dispose();
        }
        if (this.reader != null) {
            this.reader.dispose();
        }
    }

    public synchronized void start() {
        if (queue == null) {
            init();
        }
        started = true;
        drain();

        if (!settings.getBufferTimeout().isZero()) {
            if (intervalFlush != null) {
                intervalFlush.dispose();
            }
            //定时刷新
            intervalFlush = Flux
                .interval(settings.getBufferTimeout())
                .doOnNext(ignore -> intervalFlush())
                .subscribe();
        }


    }

    private void dead(Collection<Buf<T>> buf) {
        if (deadQueue.addAll(buf)) {
            // DEAD_SZIE.addAndGet(this, buf.size());
        }
    }

    private void dead(Buf<T> buf) {
        if (deadQueue.add(buf)) {
            // DEAD_SZIE.incrementAndGet(this);
        }
    }

    private void requeue(Collection<Buf<T>> buffer, boolean tryDead) {
        for (Buf<T> buf : buffer) {

            buf.retry++;

            if (tryDead && buf.retry >= settings.getMaxRetryTimes() && settings.getMaxRetryTimes() > 0) {
                dead(buf);
            } else {
                //直接写入queue,而不是使用write,等待后续有新的数据进入再重试
                if (queue.offer(buf)) {
                    // REMAINDER.incrementAndGet(this);
                    settings.getEviction().tryEviction(this);
                } else {
                    dead(buf);
                }
            }
        }
    }

    private void requeue(Collection<Buf<T>> buffer) {
        requeue(buffer, true);
    }

    private void write(Buf<T> data) {
        FileQueue<Buf<T>> queue = this.queue;
        if (isDisposed()) {
            boolean flushNow;
            try {
                flushNow = queue == null || !queue.offer(data);
            } catch (Throwable ignore) {
                flushNow = true;
            }
            if (flushNow) {
                logger.info("file queue closed,write data now:{}", data.data);
                buffer().add(data);
                flush();
            }
            return;
        }
        // remainder ++
        monitor.in();
        // REMAINDER.incrementAndGet(this);

        queue.offer(data);

        drain();

        //尝试执行淘汰策略
        settings.getEviction().tryEviction(this);
    }

    //异步写入数据到buffer
    public Mono<Void> writeAsync(T data) {
        if (isDisposed()) {
            return Mono.fromRunnable(() -> write(data));
        }
        return Mono
            .fromRunnable(() -> write(data))
            .subscribeOn(writer)
            // 切换到parallel线程池,避免浪费writer线程性能。
            // 但是线程切换本身也是消耗。
            .publishOn(Schedulers.parallel())
            .then();
    }

    //异步写入数据到buffer
    public Mono<Void> writeAsync(Collection<T> data) {
        if (isDisposed()) {
            return Mono.fromRunnable(() -> data.forEach(this::write));
        }
        return Mono
            .fromRunnable(() -> data.forEach(this::write))
            .subscribeOn(writer)
            .publishOn(Schedulers.parallel())
            .then();
    }

    //写入数据到buffer,此操作可能阻塞
    @Deprecated
    public void write(T data) {
        write(new Buf<>(data, instanceBuilder));
    }

    public void stop() {
        started = false;
        if (this.intervalFlush != null) {
            this.intervalFlush.dispose();
        }
        for (FlushSubscriber subscriber : new ArrayList<>(flushing)) {
            subscriber.doCancel();
        }
    }

    @SneakyThrows
    public void dispose() {
        logger.info("dispose buffer:{},wip:{},remainder:{}", name, wip, queue.size());
        started = false;
        if (DISPOSED.compareAndSet(this, false, true)) {
            if (this.intervalFlush != null) {
                this.intervalFlush.dispose();
            }
            int max = 16;
            long wip = WIP.get(this);
            do {
                if (wip > 0) {
                    logger.info("cancel buffer flushing...wip:{},remainder:{}", wip, queue.size());
                }
                for (FlushSubscriber subscriber : new ArrayList<>(flushing)) {
                    subscriber.doCancel();
                }

                try {
                    Thread.sleep(500);
                } catch (Throwable ignore) {
                    break;
                }
                wip = WIP.get(this);
            }
            while (wip > 0 && (max--) > 0);

            if (wip > 0) {
                logger.warn("wait buffer flushing timeout...wip:{}", wip);
            }
            @SuppressWarnings("all")
            Collection<Buf<T>> remainders = BUFFER.getAndSet(this, newBuffer());
            //写出内存中的数据
            queue.addAll(remainders);
            queue.close();
            deadQueue.close();
            queue = null;
            deadQueue = null;
            shutdownScheduler();
        }
        unregisterMbean();
    }

    @Override
    public boolean isDisposed() {
        return DISPOSED.get(this);
    }

    public long size() {
        return queue == null || disposed ? 0 : queue.size() + buffer().size();
    }

    public long size(BufferType type) {
        return type == BufferType.buffer ? size() : deadQueue == null || disposed ? 0 : deadQueue.size();
    }

    @Override
    public void removeLatest(BufferType type) {
        if (type == BufferType.buffer) {
            if (queue.removeLast() != null) {
                monitor.dropped();
            }
        } else {
            if (deadQueue.removeLast() != null) {
                // DEAD_SZIE.decrementAndGet(this);
            }
        }
    }

    @Override
    public void removeOldest(BufferType type) {
        if (type == BufferType.buffer) {
            if (queue.removeFirst() != null) {
                monitor.dropped();
            }
        } else {
            if (deadQueue.removeFirst() != null) {
                // DEAD_SZIE.decrementAndGet(this);
            }
        }
    }

    private void intervalFlush() {
        if (System.currentTimeMillis() - lastFlushTime >= settings.getBufferTimeout().toMillis()
            && WIP.get(this) <= settings.getParallelism()
            && started) {
            flush();
        }
    }

    private final Set<FlushSubscriber> flushing = ConcurrentHashMap.newKeySet();

    private void flush(Collection<Buf<T>> c) {
        try {
            lastFlushTime = System.currentTimeMillis();
            if (c.isEmpty()) {
                drain();
                return;
            }
            // wip++
            FlushSubscriber subscriber = new FlushSubscriber(c);
            handler
                .apply(Collections.unmodifiableCollection(c), subscriber)
                .subscribe(subscriber);
        } catch (Throwable e) {
            logger.warn("flush buffer error", e);
            WIP.decrementAndGet(this);
            requeue(c, true);
        }
    }

    class FlushSubscriber extends BaseSubscriber<Boolean> implements FlushContext<T> {
        final Collection<Buf<T>> buffer;
        final long startWith = System.currentTimeMillis();

        @Override
        public void error(Throwable e) {
            lastError = e;
        }

        public FlushSubscriber(Collection<Buf<T>> buffer) {
            this.buffer = buffer;
        }

        @Override
        @Nonnull
        protected Subscription upstream() {
            return super.upstream();
        }

        @Override
        protected void hookOnSubscribe(@Nonnull Subscription subscription) {
            flushing.add(this);
            WIP.incrementAndGet(PersistenceBuffer.this);
            super.hookOnSubscribe(subscription);
        }

        @Override
        protected void hookOnNext(@Nonnull Boolean doRequeue) {
            synchronized (this) {
                if (isDisposed()) {
                    return;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("write {} data,size:{},remainder:{},requeue: {}.take up time: {} ms",
                                 name,
                                 buffer.size(),
                                 size(),
                                 doRequeue,
                                 System.currentTimeMillis() - startWith);
                }
                if (doRequeue) {
                    //重试,但是不触发dead
                    requeue(takeRetryBuffer(), false);
                    //手动指定了dead的数据
                    dead(takeDeadBuffer(false));
                } else {
                    lastError = null;
                }
            }
        }

        @Override
        protected void hookOnError(@Nonnull Throwable err) {
            synchronized (this) {
                lastError = err;
                if (settings.getRetryWhenError().test(err)) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("write {} data failed do retry later,size:{},remainder:{}.use time: {} ms,error: {}",
                                    name,
                                    buffer.size(),
                                    size(),
                                    System.currentTimeMillis() - startWith,
                                    ExceptionUtils.getMessage(err));
                    }
                    requeue(takeRetryBuffer());
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn("write {} data error,size:{},remainder:{}.use time: {} ms",
                                    name,
                                    buffer.size(),
                                    size(),
                                    System.currentTimeMillis() - startWith,
                                    err);
                    }
                    dead(takeDeadBuffer(true));
                }
            }
        }

        @Override
        protected void hookOnCancel() {
            // hookOnNext(true);
        }

        void doCancel() {

            synchronized (this) {
                if (this.isDisposed()) {
                    return;
                }
                List<Buf<T>> buf = new ArrayList<>(buffer);
                buffer.clear();

                upstream().cancel();
                cancel();

                queue.addAll(buf);
                //  REMAINDER.addAndGet(PersistenceBuffer.this, buffer.size());
            }
        }

        private Collection<Buf<T>> takeRetryBuffer() {
            return Collections2.filter(
                this.buffer,
                //指定了retry并且没有dead
                buf -> ((buf.doRetry == null) || buf.doRetry) &&
                    (buf.doDead == null || !buf.doDead));
        }

        private Collection<Buf<T>> takeDeadBuffer(boolean defaultDead) {
            return Collections2.filter(
                this.buffer,
                //没有指定重试并且指定了dead
                buf -> (buf.doRetry == null || !buf.doRetry)
                    && ((defaultDead && buf.doDead == null) || Boolean.TRUE.equals(buf.doDead)));
        }

        @Override
        protected void hookFinally(@Nonnull SignalType type) {
            int size = buffer.size();
            for (Buf<T> tBuf : buffer) {
                tBuf.reset();
            }
            buffer.forEach(Buf::reset);
            flushing.remove(this);
            monitor.out(size, System.currentTimeMillis() - startWith);
            // wip--
            WIP.decrementAndGet(PersistenceBuffer.this);
            drain();
        }

    }

    private void flush() {
        @SuppressWarnings("all")
        Collection<Buf<T>> c = BUFFER.getAndSet(this, newBuffer());
        flush(c);
    }

    private Collection<Buf<T>> newBuffer() {
        return new ArrayList<>(settings.getBufferSize());
    }

    private void drain() {
        if (!started) {
            return;
        }
        // 当前未执行完成的操作小于并行度才请求
        if (WIP.incrementAndGet(this) <= settings.getParallelism()) {
            // 使用boundedElastic线程执行poll,避免阻塞线程
            reader
                .schedule(() -> {
                    int size = settings.getBufferSize();
                    for (int i = 0; i < size && started; i++) {
                        Buf<T> poll = settings.getStrategy() == ConsumeStrategy.LIFO
                            ? queue.removeLast()
                            : queue.poll();
                        if (poll != null) {
                            onNext(poll);
                        } else {
                            break;
                        }
                    }
                    WIP.decrementAndGet(this);
                });
        } else {
            WIP.decrementAndGet(this);
        }
    }

    private void onNext(@Nonnull Buf<T> value) {

        Collection<Buf<T>> c;
        boolean flush = false;

        synchronized (this) {
            c = buffer();
            if (c.size() == settings.getBufferSize() - 1) {
                BUFFER.compareAndSet(this, c, newBuffer());
                flush = true;
            }
            c.add(value);
        }
        if (flush) {
            flush(c);
        }

    }

    @SuppressWarnings("unchecked")
    private Collection<Buf<T>> buffer() {
        return BUFFER.get(this);
    }

    @SneakyThrows
    protected ObjectInput createInput(ByteBuf buffer) {
        return Serializers.getDefault().createInput(new ByteBufInputStream(buffer, true));
    }

    @SneakyThrows
    protected ObjectOutput createOutput(ByteBuf buffer) {
        return Serializers.getDefault().createOutput(new ByteBufOutputStream(buffer));
    }

    @AllArgsConstructor
    public static class Buf<T> implements Buffered<T>, Externalizable {
        private final Supplier<Externalizable> instanceBuilder;
        private T data;
        private int retry = 0;
        private Boolean doRetry, doDead;

        @SneakyThrows
        public Buf() {
            throw new IllegalAccessException();
        }

        public Buf(Supplier<Externalizable> instanceBuilder) {
            this.instanceBuilder = instanceBuilder;
        }

        public Buf(T data, Supplier<Externalizable> instanceBuilder) {
            this.data = data;
            this.instanceBuilder = instanceBuilder;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(retry);
            if (instanceBuilder != null) {
                ((Externalizable) data).writeExternal(out);
            } else {
                SerializeUtils.writeObject(data, out);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            retry = in.readInt();
            if (instanceBuilder != null) {
                Externalizable data = instanceBuilder.get();
                data.readExternal(in);
                this.data = (T) data;
            } else {
                this.data = (T) SerializeUtils.readObject(in);
            }
        }

        @Override
        public T getData() {
            return data;
        }

        @Override
        public int getRetryTimes() {
            return retry;
        }

        @Override
        public void retry(boolean retry) {
            doRetry = retry;
        }

        @Override
        public void dead() {
            doDead = true;
        }

        private void reset() {
            doRetry = null;
            doDead = null;
        }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }

    //尝试恢复文件数据
    public long recovery(String fileName, boolean dead) {
        if (fileName.startsWith("../")) {
            return 0;
        }
        if (Objects.equals(fileName, settings.getFileName()) ||
            Objects.equals(fileName, settings.getFileName() + ".dead")) {
            return 0;
        }
        File file = new File(settings.getFilePath(), fileName);
        if (!file.isFile() || !file.exists()) {
            return 0;
        }
        BufDataType dataType = newType();
        //数据队列
        FileQueue<Buf<T>> _queue = wrap(
            FileQueue
                .<Buf<T>>builder()
                .name(fileName)
                .path(Paths.get(settings.getFilePath()))
                .option("valueType", dataType)
                .build());
        try {
            long size = _queue.size();
            if (dead) {
                queue.addAll(_queue);
                PersistenceBuffer.REMAINDER.addAndGet(this, size);
            } else {
                deadQueue.addAll(_queue);
                PersistenceBuffer.DEAD_SZIE.addAndGet(this, size);
            }
            return size;
        } finally {
            _queue.close();
        }
    }

    BufDataType newType() {
        return new BufDataType();
    }

    class BufDataType extends BasicDataType<Buf<T>> {

        @Override
        public int compare(Buf<T> a, Buf<T> b) {
            return 0;
        }

        @Override
        public int getMemory(Buf<T> obj) {
            if (obj.data instanceof MemoryUsage) {
                return ((MemoryUsage) obj.data).usage();
            }
            if (obj.data instanceof String) {
                return ((String) obj.data).length() * 2;
            }
            return 4096;
        }

        @Override
        @SneakyThrows
        public void write(WriteBuffer buff, Buf<T> data) {
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try (ObjectOutput output = createOutput(buffer)) {
                data.writeExternal(output);
                output.flush();
                buff.put(buffer.nioBuffer());
            } finally {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }

        @Override
        @SneakyThrows
        public void write(WriteBuffer buff, Object obj, int len) {
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try (ObjectOutput output = createOutput(buffer)) {
                for (int i = 0; i < len; i++) {
                    @SuppressWarnings("all")
                    Buf<T> buf = ((Buf<T>) Array.get(obj, i));
                    buf.writeExternal(output);
                }
                output.flush();
                buff.put(buffer.nioBuffer());
            } finally {
                ReferenceCountUtil.safeRelease(buffer);
            }

        }

        @Override
        @SneakyThrows
        public void read(ByteBuffer buff, Object obj, int len) {
            try (ObjectInput input = createInput(Unpooled.wrappedBuffer(buff))) {
                for (int i = 0; i < len; i++) {
                    Buf<T> data = new Buf<>(instanceBuilder);
                    data.readExternal(input);
                    Array.set(obj, i, data);
                }
            }
        }

        @Override
        @SneakyThrows
        public Buf<T> read(ByteBuffer buff) {
            Buf<T> data = new Buf<>(instanceBuilder);
            try (ObjectInput input = createInput(Unpooled.wrappedBuffer(buff))) {
                data.readExternal(input);
            }
            return data;
        }


        @Override
        public Buf<T>[] createStorage(int size) {
            return new Buf[size];
        }
    }

    private ObjectName objectName;

    void registerMbean() {
        try {
            String safeName = name.replaceAll("[\\s\\\\/:*?\"<>|]", "_");
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            objectName = new ObjectName("org.jetlinks:type=PersistenceBuffer,name=" + safeName);
            mBeanServer.registerMBean(new StandardMBean(monitor, PersistenceBufferMBean.class), objectName);
        } catch (Throwable error) {
            logger.warn("registerMBean {} error ", name, error);
        }
    }

    void unregisterMbean() {
        try {
            if (objectName != null) {
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Throwable ignore) {
        }
    }


    @RequiredArgsConstructor
    private static class PersistenceBufferMBeanImpl<T extends Serializable> implements PersistenceBufferMBean {
        @SuppressWarnings("all")
        private static final AtomicLongFieldUpdater<PersistenceBufferMBeanImpl>
            IN = AtomicLongFieldUpdater.newUpdater(PersistenceBufferMBeanImpl.class, "in"),
            OUT = AtomicLongFieldUpdater.newUpdater(PersistenceBufferMBeanImpl.class, "out"),
            COST = AtomicLongFieldUpdater.newUpdater(PersistenceBufferMBeanImpl.class, "cost"),
            OPT = AtomicLongFieldUpdater.newUpdater(PersistenceBufferMBeanImpl.class, "opt"),
            DROPPED = AtomicLongFieldUpdater.newUpdater(PersistenceBufferMBeanImpl.class, "dropped");

        private final PersistenceBuffer<T> buffer;

        private volatile long
            //写入数量
            in,
        //写出数量
        out,
        //写出次数
        opt,
        //写出总耗时
        cost,
        //淘汰数量
        dropped;

        private final long[] costDist = new long[5];

        private void dropped() {
            DROPPED.incrementAndGet(this);
        }

        private void in() {
            IN.incrementAndGet(this);
        }

        private void out(long outSize, long cost) {
            COST.addAndGet(this, cost);
            OUT.addAndGet(this, outSize);
            OPT.incrementAndGet(this);
            if (cost < 50) {
                costDist[0]++;
            } else if (cost < 200) {
                costDist[1]++;
            } else if (cost < 1000) {
                costDist[2]++;
            } else if (cost < 5000) {
                costDist[3]++;
            } else {
                costDist[4]++;
            }
        }

        @Override
        public String getMonitor() {
            return String.format(
                "\nqueue(in %s,out %s,dropped %s);\nconsume(opt %s,cost %s ms);\ndist[0-50ms(%s),50-200ms(%s),0.2-1s(%s),1-5s(%s),>5s(%s)]\n",
                in, out, dropped, opt, cost, costDist[0], costDist[1], costDist[2], costDist[3], costDist[4]);
        }

        @Override
        public void resetMonitor() {
            IN.set(this, 0);
            OUT.set(this, 0);
            OPT.set(this, 0);
            COST.set(this, 0);
            Arrays.fill(costDist, 0);
        }

        @Override
        public long getRemainder() {
            return buffer.queue.size();
        }

        @Override
        public long getDeadSize() {
            return buffer.deadQueue.size();
        }

        @Override
        public long getWip() {
            return buffer.wip;
        }

        @Override
        public String getLastError() {
            Throwable error = buffer.lastError;
            return error == null ? "nil"
                : ExceptionUtils.getRootCauseMessage(error) + ":" + ExceptionUtils.getStackTrace(error);
        }

        @Override
        public String getStoragePath() {
            return buffer.settings.getFilePath();
        }

        @Override
        public List<String> getDataBytes() {
            File[] files = new File(buffer.settings.getFilePath())
                .listFiles(filter -> filter.getName().startsWith(getSafeFileName(buffer.settings.getFileName())));
            if (files == null) {
                return Collections.emptyList();
            }

            return Arrays
                .stream(files)
                .map(file -> file.getName() + " " + FormatUtils.formatDataSize(file.length()))
                .collect(Collectors.toList());
        }

        @Override
        public void flush() {
            buffer.queue.flush();
            buffer.deadQueue.flush();
        }

        @Override
        public void retryDead(int maxSize) {
            //单次请求最大重试次数
            maxSize = Math.min(50_0000, maxSize);
            while (maxSize-- > 0) {
                Buf<T> buf = buffer.deadQueue.poll();
                if (buf == null) {
                    break;
                }
                buf.retry = 0;
                if (!buffer.queue.offer(buf)) {
                    buffer.deadQueue.offer(buf);
                    break;
                }
            }
            buffer.drain();
        }

        @Override
        public String getSettings() {
            return String.format("\nbufferSize: %s" +
                                     ",bufferTimeout: %s" +
                                     ",parallelism: %s" +
                                     ",maxRetryTimes: %s" +
                                     ",fileConcurrency: %s" + "\nEviction:%s ",
                                 buffer.settings.getBufferSize(),
                                 buffer.settings.getBufferTimeout(),
                                 buffer.settings.getParallelism(),
                                 buffer.settings.getMaxRetryTimes(),
                                 buffer.settings.getFileConcurrency(),
                                 buffer.settings.getEviction());
        }

        @Override
        public long recovery(String fileName, boolean dead) {
            return buffer.recovery(fileName, dead);
        }

        @Override
        public List<Object> peekDead(int size) {
            size = size <= 0 ? 1 : Math.min(1024, size);

            List<Object> result = new ArrayList<>(size);

            for (Buf<T> tBuf : buffer.deadQueue) {
                if (size-- <= 0) {
                    break;
                }
                result.add(tBuf.data);
            }

            return result;
        }
    }

    public interface PersistenceBufferMBean {

        String getSettings();

        String getMonitor();

        void resetMonitor();

        String getStoragePath();

        long getRemainder();

        long getDeadSize();

        long getWip();

        String getLastError();

        List<String> getDataBytes();

        void flush();

        void retryDead(int maxSize);

        long recovery(String fileName, boolean dead);

        List<Object> peekDead(int size);
    }

    public interface FlushContext<T> {

        //标记错误信息
        void error(Throwable e);
    }
}
