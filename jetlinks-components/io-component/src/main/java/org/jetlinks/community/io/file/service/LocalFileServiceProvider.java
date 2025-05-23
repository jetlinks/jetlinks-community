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
package org.jetlinks.community.io.file.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.scalecube.services.annotations.ServiceMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.community.io.file.*;
import org.jetlinks.community.io.file.info.ShardingUploadResult;
import org.jetlinks.community.io.file.info.UploadResponse;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.core.trace.MonoTracer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.*;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.io.*;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;


@Slf4j
public class LocalFileServiceProvider implements FileServiceProvider {

    public static final String TYPE = "local";

    private final FileProperties properties;

    private final NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    private final ReactiveRedisTemplate<String, String> redis;

    public LocalFileServiceProvider(RpcManager rpcManager,
                                    FileProperties properties,
                                    ReactiveRedisTemplate<String, String> redis) {
        new File(properties.getStorageBasePath()).mkdirs();
        this.properties = properties;
        this.redis = redis;
        rpcManager.registerService(new ServiceImpl());
    }


    static String redisKey(String chunkId) {
        return "file-uploading:" + chunkId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Mono<ShardingUploadResult> saveFile(String sessionId,
                                               String fileId,
                                               String path, long length, long offset,
                                               Flux<DataBuffer> stream,
                                               FileOption... options) {
        Objects.requireNonNull(sessionId, "sessionId can not be null");
        ShardingUploader uploader = new ShardingUploader(sessionId, fileId, path, length, offset, options);

        return uploader.doSaveFile(stream);
    }

    static void closeChannel(@Nullable Channel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public Mono<UploadResponse> saveFile(String storagePath, Flux<DataBuffer> stream, FileOption... options) {
        String storageBasePath = properties.getStorageBasePath();
        Path path = Paths.get(storageBasePath, storagePath);
        path.toFile().getParentFile().mkdirs();
        MessageDigest md5 = DigestUtils.getMd5Digest();
        MessageDigest sha256 = DigestUtils.getSha256Digest();
        return stream
            .map(buffer -> FileUtils.updateDigest(md5, FileUtils.updateDigest(sha256, buffer)))
            .as(buf -> DataBufferUtils
                .write(buf, path,
                       StandardOpenOption.WRITE,
                       CREATE_NEW,
                       StandardOpenOption.TRUNCATE_EXISTING))
            .then(Mono.defer(() -> {
                File savedFile = path.toFile();
                if (!savedFile.exists()) {
                    return Mono.error(new BusinessException("error.file_storage_failed"));
                }
                UploadResponse response = new UploadResponse();
                response.setMd5(ByteBufUtil.hexDump(md5.digest()));
                response.setSha256(ByteBufUtil.hexDump(sha256.digest()));
                response.setLength(savedFile.length());
                return Mono.just(response);
            }));
    }

    @Override
    public Flux<DataBuffer> read(FileInfo fileInfo, Function<FileManager.ReaderContext, Mono<Void>> beforeRead) {
        if (fileInfo.getLength() == 0) {
            //未获取到文件长度，尝试获取
            long length = Paths.get(properties.getStorageBasePath(), fileInfo.getPath()).toFile().length();
            fileInfo.setLength(length);
        }
        DefaultReaderContext context = new DefaultReaderContext(fileInfo, 0, fileInfo.getLength());
        return beforeRead
            .apply(context)
            .thenMany(Flux.defer(() -> readFile(fileInfo, context.getPosition(), context.getLength())));
    }

    @Override
    public Mono<Void> delete(String storagePath) {
        return deleteLocal(storagePath).then();
    }

    private Mono<Boolean> deleteLocal(String storagePath) {
        return Mono
            .fromCallable(() -> {
                File file = Paths.get(properties.getStorageBasePath(), storagePath).toFile();
                if (file.exists()) {
                    log.debug("delete file: {}", file.getAbsolutePath());
                    return file.delete();
                }
                return false;
            });
    }

    private Flux<DataBuffer> readFile(FileInfo file, long position, long length) {
        if (length == 0) {
            return Flux.empty();
        }
        return readFile(file.getPath(), position, file.getLength() == length ? -1 : length);
    }

    protected Flux<DataBuffer> readFile(String filePath, long position, long length) {
        return DataBufferUtils
            .read(new FileSystemResource(Paths.get(properties.getStorageBasePath(), filePath)),
                  position,
                  bufferFactory,
                  (int) properties.getReadBufferSize().toBytes())
            .as(flux -> length <= 0 ? flux : truncate(flux, length))
            .onErrorMap(NoSuchFileException.class,
                        e -> new BusinessException.NoStackTrace("error.file_does_not_exist")
                            .withSource("readFile", filePath));
    }

    private Flux<DataBuffer> truncate(Flux<DataBuffer> data, long length) {
        return Flux.defer(() -> {
            AtomicLong countdown = new AtomicLong(length);
            return data
                .map(buffer -> {
                    long bytesCount = buffer.readableByteCount();
                    long remainder = countdown.addAndGet(-bytesCount);
                    if (remainder < 0) {
                        return buffer.slice(buffer.readPosition(), (int) (bytesCount + remainder));
                    } else {
                        return buffer;
                    }
                })
                .takeUntil(buffer -> countdown.get() <= 0);
        }).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
    }

    @Override
    public int getOrder() {
        return -100;
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReadRequest {
        private String filePath;
        private long position;
        private long length;
    }

    static final RedisScript<Object> script = RedisScript.of(
        "local redisKey = KEYS[1] \n" +
            "local offsetKey = ARGV[1] \n" +
            "local offsetValue = ARGV[2] \n" +
            "local totalLength = tonumber(ARGV[3]) \n" +
            "local exists = redis.call(\"HEXISTS\", redisKey, offsetKey)\n" +
            "if exists == 0 then \n" +
            "    redis.call(\"HSET\", redisKey, offsetKey, offsetValue)\n" +
            "    redis.call(\"HINCRBY\", redisKey,\"length\", tonumber(offsetValue))\n" +
            "end \n" +
            "local currentLength = redis.call(\"HGET\", redisKey, \"length\")\n" +
            "if tonumber(currentLength) == totalLength then \n" +
            "    redis.call('DEL', redisKey) \n" +  // 如果等于总长度，删除redisKey
            "    return 'true' \n" +
            "else \n" +
            "    return 'false' \n" +
            "end \n", Object.class);

    class ShardingUploader implements Externalizable {
        String chunkId;
        String fileId;
        long length;
        String path;
        FileOption[] options;
        long offset;

        private transient String realFileId;
        private transient String storagePath;
        private transient UploadResponse response;
        private transient String redisKey;
        private transient Path file;
        private transient ReactiveHashOperations<String, String, String> hash;

        public ShardingUploader(String chunkId, String fileId, String path, long length, long offset, FileOption... options) {
            this.chunkId = chunkId;
            this.fileId = fileId;
            this.length = length;
            this.path = path;
            this.offset = offset;
            this.options = options;
        }

        public ShardingUploader() {
        }


        private Mono<Void> computeDigest() {
            MessageDigest md5 = DigestUtils.getMd5Digest();
            MessageDigest sha256 = DigestUtils.getSha256Digest();
            AtomicLong totalSize = new AtomicLong(0);
            //计算完整文件的md5和sha256，文件长度
            return DataBufferUtils
                .read(file,
                      bufferFactory,
                      8 * 1024 * 1024)
                .map(buffer -> FileUtils.updateDigest(md5, FileUtils.updateDigest(sha256, buffer)))
                .doOnNext(buffer -> totalSize.addAndGet(buffer.readableByteCount()))
                .doOnNext(DataBufferUtils::release)
                .then(Mono.fromRunnable(() -> {
                    response.setMd5(ByteBufUtil.hexDump(md5.digest()));
                    response.setSha256(ByteBufUtil.hexDump(sha256.digest()));
                    response.setLength(totalSize.get());
                }));
        }

        @SuppressWarnings("all")
        private Mono<Long> doWrite(Flux<DataBuffer> stream) {
            return Mono
                .<Long>create(sink -> {
                    try {
                        AsynchronousFileChannel channel = AsynchronousFileChannel
                            .open(file, Sets.newHashSet(StandardOpenOption.WRITE, CREATE), null);
                        sink.onDispose(() -> closeChannel(channel));
                        AtomicLong ref = new AtomicLong();
                        MessageDigest md5 = DigestUtils.getMd5Digest();
                        MessageDigest sha256 = DigestUtils.getSha256Digest();
                        DataBufferUtils
                            .write(stream.map(buffer -> FileUtils.updateDigest(md5, FileUtils.updateDigest(sha256, buffer))), channel, offset)
                            .subscribe(buffer -> {
                                           ref.addAndGet(buffer.writePosition());
                                           DataBufferUtils.release(buffer);
                                       },
                                       sink::error,
                                       () -> {
                                           response.setMd5(ByteBufUtil.hexDump(md5.digest()));
                                           response.setSha256(ByteBufUtil.hexDump(sha256.digest()));
                                           response.setLength(ref.get());
                                           sink.success(ref.get());
                                       },
                                       Context.of(sink.contextView()));
                    } catch (IOException ex) {
                        sink.error(ex);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .as(MonoTracer.create(
                    "/file-sharding/uploading/" + chunkId,
                    (span, val) -> {
                        span.setAttribute("md5", response.getMd5());
                        span.setAttribute("sha256", response.getSha256());
                        span.setAttribute("offset", offset);
                        span.setAttribute("size", val);
                        span.setAttribute("file", file.toString());
                    }));
        }

        private Mono<Void> newFile() {
            return Mono.defer(() -> {
                return hash
                    .putIfAbsent(redisKey, "path", response.getPath())
                    .flatMap(ignore -> hash.get(redisKey, "path"))
                    .doOnNext(path -> {
                        Path file = Paths.get(properties.getStorageBasePath(), path);
                        file.toFile().getParentFile().mkdirs();
                        this.storagePath = path;
                        this.file = file;
                    })
                    .then();
            });
        }

        private Mono<Void> prepare() {
            redisKey = redisKey(chunkId);
            response = new UploadResponse();
            response.setLength(length);
            response.setOptions(options);
            response.setPath(path);
            hash = redis.opsForHash();

            return hash
                .putIfAbsent(redisKey, "id", fileId)
                .flatMap(ignore -> hash.get(redisKey, "id"))
                .doOnNext(id -> realFileId = id)
                .then(newFile());
        }


        private Mono<Boolean> isComplete(long length) {
            return redis
                .execute(script,
                         Collections.singletonList(redisKey),
                         Lists.newArrayList("offset-" + offset, String.valueOf(length), String.valueOf(this.length)))
                .take(1)
                .singleOrEmpty()
                .map(complete -> Boolean.parseBoolean(String.valueOf(complete)));
        }

        public Mono<ShardingUploadResult> doSaveFile(Flux<DataBuffer> stream) {
            return this
                .prepare()
                .then(Mono.defer(() -> this.doWrite(stream)))
                .flatMap(this::isComplete)
                .flatMap(completed -> {
                    //更新path,避免后续上传与首次上传生成path不一致
                    response.setPath(storagePath);
                    if (completed) {
                        return redis
                            .delete(redisKey)
                            .then(computeDigest())
                            .then(Mono.defer(() -> Mono.just(new ShardingUploadResult(true, realFileId, response))));
                    } else {
                        return Mono.just(new ShardingUploadResult(false, realFileId, response));
                    }

                });
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(chunkId);
            out.writeUTF(fileId);
            out.writeUTF(path);
            out.writeLong(length);
            out.writeLong(offset);
            if (options != null) {
                out.writeByte(options.length);
                for (FileOption option : options) {
                    out.writeByte(option.ordinal());
                }
            } else {
                out.writeByte(0);
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            chunkId = in.readUTF();
            fileId = in.readUTF();
            path = in.readUTF();
            length = in.readLong();
            offset = in.readLong();

            int len = in.readUnsignedByte();
            if (len > 0) {
                options = new FileOption[len];
                for (int i = 0; i < len; i++) {
                    options[i] = FileOption.all[in.readByte()];
                }
            }
        }
    }

    @io.scalecube.services.annotations.Service
    public interface Service {

        @ServiceMethod
        Mono<Boolean> existFile(String storagePath);

        @ServiceMethod
        Flux<ByteBuf> read(ReadRequest request);

        //为什么不能返回Mono? 因为目前RPC底层还不支持...
        @ServiceMethod
        Flux<ShardingUploadResult> write(Flux<ByteBuf> stream);

        @ServiceMethod
        Mono<Boolean> delete(String storagePath);
    }


    public class ServiceImpl implements Service {
        @Override
        public Mono<Boolean> existFile(String storagePath) {
            return LocalFileServiceProvider.this.existFile(storagePath);
        }

        @Override
        public Flux<ShardingUploadResult> write(Flux<ByteBuf> stream) {

            return stream
                .switchOnFirst((signal, byteBufFlux) -> {
                    if (signal.hasValue()) {
                        ByteBuf buf = signal.get();
                        ShardingUploader request = Serializers.deserializeExternal(buf, ShardingUploader::new);
                        return request.doSaveFile(byteBufFlux.skip(1).map(bufferFactory::wrap));
                    } else {
                        return Mono.empty();
                    }
                });
        }

        @Override
        public Mono<Boolean> delete(String storagePath) {
            return LocalFileServiceProvider.this.deleteLocal(storagePath);
        }

        @Override
        public Flux<ByteBuf> read(ReadRequest request) {
            return LocalFileServiceProvider
                .this
                .readFile(request.filePath, request.position, request.length)
                .map(ConverterUtils::convertBuffer);
        }
    }

    public Mono<Boolean> existFile(String storagePath) {
        return Mono.fromCallable(() -> {
            File file = Paths.get(properties.getStorageBasePath(), storagePath).toFile();
            return file.exists();
        });
    }

}
