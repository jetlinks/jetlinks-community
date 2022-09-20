package org.jetlinks.community.buffer;

import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.core.cache.FileQueue;
import org.jetlinks.core.cache.FileQueueProxy;
import org.jetlinks.core.utils.SerializeUtils;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
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
    private final static AtomicIntegerFieldUpdater<PersistenceBuffer> REMAINDER =
        AtomicIntegerFieldUpdater.newUpdater(PersistenceBuffer.class, "remainder");

    @SuppressWarnings("all")
    private final static AtomicIntegerFieldUpdater<PersistenceBuffer> DEAD_SZIE =
        AtomicIntegerFieldUpdater.newUpdater(PersistenceBuffer.class, "deadSize");

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
    private final Function<Flux<T>, Mono<Boolean>> handler;

    //缓冲区大小,超过此大小将执行 handler 处理逻辑
    private BufferSettings settings;
    //缓冲区
    private volatile Collection<Buf<T>> buffer;

    //反序列化时指定快速实例化
    private final Supplier<Externalizable> instanceBuilder;

    //上一次刷新时间
    private long lastFlushTime;

    //当前正在进行的操作
    private volatile int wip;

    //剩余数量
    private volatile int remainder;

    //死数据数量
    private volatile int deadSize;

    //刷新缓冲区定时任务
    private Disposable intervalFlush;

    private volatile Boolean disposed = false;

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
                             Function<Flux<T>, Mono<Boolean>> handler) {
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
        //包装一层,防止apply直接报错导致流中断
        this.handler = list -> Mono
            .defer(() -> handler.apply(list));

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

    private void init() {
        String filePath = settings.getFilePath();
        String fileName = settings.getFileName();
        Path path = Paths.get(filePath);

        fileName = fileName.replaceAll("[\\s\\\\/:*?\"<>|]", "_");

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

    public void start() {
        if (intervalFlush != null) {
            return;
        }

        init();

        drain();

        if (!settings.getBufferTimeout().isZero()) {
            //定时刷新
            intervalFlush = Flux
                .interval(settings.getBufferTimeout())
                .doOnNext(ignore -> intervalFlush())
                .subscribe();
        }


    }

    private void dead(Collection<Buf<T>> buf) {
        if (deadQueue.addAll(buf)) {
            DEAD_SZIE.addAndGet(this, buf.size());
        }
    }

    private void dead(Buf<T> buf) {
        if (deadQueue.add(buf)) {
            DEAD_SZIE.incrementAndGet(this);
        }
    }

    private void requeue(Collection<Buf<T>> buffer) {
        for (Buf<T> buf : buffer) {
            if (++buf.retry >= settings.getMaxRetryTimes()) {
                dead(buf);
            } else {
                //直接写入queue,而不是使用write,等待后续有新的数据进入再重试
                if (queue.offer(buf)) {
                    REMAINDER.incrementAndGet(this);
                }
            }
        }
    }

    private void write(Buf<T> data) {
        // remainder ++
        REMAINDER.incrementAndGet(this);

        queue.offer(data);

        drain();
    }

    public void write(T data) {
        write(new Buf<>(data, instanceBuilder));
    }

    public void dispose() {
        if (DISPOSED.compareAndSet(this, false, true)) {
            if (this.intervalFlush != null) {
                this.intervalFlush.dispose();
            }
            //写出内存中的数据
            queue.addAll(BUFFER.getAndSet(this, newBuffer()));
            queue.close();
            deadQueue.close();
        }
    }

    @Override
    public boolean isDisposed() {
        return DISPOSED.get(this);
    }

    public int size() {
        return remainder;
    }

    private void intervalFlush() {
        if (System.currentTimeMillis() - lastFlushTime >= settings.getBufferTimeout().toMillis()
            && WIP.get(this) <= settings.getParallelism()) {
            flush();
        }
    }

    private void flush(Collection<Buf<T>> c) {
        try {
            lastFlushTime = System.currentTimeMillis();
            if (c.isEmpty()) {
                drain();
                return;
            }
            // wip++
            WIP.incrementAndGet(this);

            handler
                .apply(Flux.fromIterable(c).mapNotNull(buf -> buf.data))
                .subscribe(new BaseSubscriber<Boolean>() {
                    final long startWith = System.currentTimeMillis();
                    final int remainder = REMAINDER.get(PersistenceBuffer.this);

                    @Override
                    protected void hookOnNext(@Nonnull Boolean doRequeue) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("write {} data,size:{},remainder:{},requeue: {}.take up time: {} ms",
                                         name,
                                         c.size(),
                                         remainder,
                                         doRequeue,
                                         System.currentTimeMillis() - startWith);
                        }
                        if (doRequeue) {
                            requeue(c);
                        }
                    }

                    @Override
                    protected void hookOnError(@Nonnull Throwable err) {
                        if (settings.getRetryWhenError().test(err)) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("write {} data failed do retry later,size:{},remainder:{}.use time: {} ms",
                                            name,
                                            c.size(),
                                            remainder,
                                            System.currentTimeMillis() - startWith);
                            }
                            requeue(c);
                        } else {
                            if (logger.isWarnEnabled()) {
                                logger.warn("write {} data error,size:{},remainder:{}.use time: {} ms",
                                            name,
                                            c.size(),
                                            remainder,
                                            System.currentTimeMillis() - startWith,
                                            err);
                            }
                            dead(c);
                        }
                    }

                    @Override
                    protected void hookFinally(@Nonnull SignalType type) {
                        // wip--
                        WIP.decrementAndGet(PersistenceBuffer.this);
                        drain();
                    }
                });
        } catch (Throwable e) {
            logger.warn("flush buffer error", e);
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
        REMAINDER.decrementAndGet(this);

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
    public static class Buf<T> implements Externalizable {
        private final Supplier<Externalizable> instanceBuilder;
        private T data;
        private int retry = 0;

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

}
