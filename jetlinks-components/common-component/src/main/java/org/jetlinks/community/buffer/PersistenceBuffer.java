package org.jetlinks.community.buffer;

import com.google.common.collect.Collections2;
import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.core.cache.FileQueue;
import org.jetlinks.core.cache.FileQueueProxy;
import org.jetlinks.core.utils.SerializeUtils;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import javax.annotation.Nonnull;
import java.io.*;
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

/**
 * 支持持久化的缓存批量操作工具,用于支持数据的批量操作,如批量写入数据到数据库等.
 * <p>
 * 数据将保存在一个文件队列里,如果写入速度跟不上,数据将会尝试写入到本地文件中.
 *
 * <pre>{@code
 *
 *    BufferWriter<Data> writer = BufferWriter
 *    .<Data>create(
 *       "./data/buffer", //文件目录
 *      "my-data.queue", //文件名
 *      buffer->{
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
 * @since pro 2.0
 */
public class PersistenceBuffer<T extends Serializable> implements Disposable {
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

    private Throwable lastError;

    private volatile Boolean disposed = false;
    private boolean started = false;

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

        queue.offer(data);

        drain();
    }

    public void write(T data) {
        write(new Buf<>(data, instanceBuilder));
    }

    public void stop() {
        started = false;
        if (this.intervalFlush != null) {
            this.intervalFlush.dispose();
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
        }
    }

    @Override
    public boolean isDisposed() {
        return DISPOSED.get(this);
    }

    public long size() {
        return queue == null ? 0 : queue.size();
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
                                 queue.size(),
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
        //当前未执行完成的操作小于并行度才请求
        if (WIP.incrementAndGet(this) <= settings.getParallelism()) {
            int size = settings.getBufferSize();
            for (int i = 0; i < size; i++) {
                if (isDisposed()) {
                    break;
                }
                Buf<T> poll = queue.poll();
                if (poll != null) {
                    onNext(poll);
                } else {
                    break;
                }
            }
        }
        WIP.decrementAndGet(this);
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
            return 10_000;
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

    public interface FlushContext<T> {

        //标记错误信息
        void error(Throwable e);
    }
}
