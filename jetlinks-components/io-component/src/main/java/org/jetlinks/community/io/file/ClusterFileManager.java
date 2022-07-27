package org.jetlinks.community.io.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.scalecube.services.annotations.ServiceMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.rpc.RpcManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.*;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Function;


public class ClusterFileManager implements FileManager {

    private final FileProperties properties;

    private final NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    private final ReactiveRepository<FileEntity, String> repository;

    private final RpcManager rpcManager;

    public ClusterFileManager(RpcManager rpcManager,
                              FileProperties properties,
                              ReactiveRepository<FileEntity, String> repository) {
        new File(properties.getStorageBasePath()).mkdirs();
        this.properties = properties;
        this.rpcManager = rpcManager;
        this.repository = repository;
        rpcManager.registerService(new ServiceImpl());
    }

    @Override
    public Mono<FileInfo> saveFile(FilePart filePart, FileOption... options) {
        return saveFile(filePart.filename(), filePart.content());
    }

    private DataBuffer updateDigest(MessageDigest digest, DataBuffer dataBuffer) {
        dataBuffer = DataBufferUtils.retain(dataBuffer);
        digest.update(dataBuffer.asByteBuffer());
        DataBufferUtils.release(dataBuffer);
        return dataBuffer;
    }

    public Mono<FileInfo> doSaveFile(String name, Flux<DataBuffer> stream, FileOption... options) {
        LocalDate now = LocalDate.now();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(IDGenerator.MD5.generate());
        fileInfo.withFileName(name);

        String storagePath = now.format(DateTimeFormatter.BASIC_ISO_DATE)
            + "/" + fileInfo.getId() + "." + fileInfo.getExtension();

        MessageDigest md5 = DigestUtils.getMd5Digest();
        MessageDigest sha256 = DigestUtils.getSha256Digest();
        String storageBasePath = properties.getStorageBasePath();
        String serverNodeId = rpcManager.currentServerId();
        Path path = Paths.get(storageBasePath, storagePath);
        path.toFile().getParentFile().mkdirs();
        return stream
            .map(buffer -> updateDigest(md5, updateDigest(sha256, buffer)))
            .as(buf -> DataBufferUtils
                .write(buf, path,
                       StandardOpenOption.WRITE,
                       StandardOpenOption.CREATE_NEW,
                       StandardOpenOption.TRUNCATE_EXISTING))
            .then(Mono.defer(() -> {
                File savedFile = Paths.get(storageBasePath, storagePath).toFile();
                if (!savedFile.exists()) {
                    return Mono.error(new BusinessException("error.file_storage_failed"));
                }
                fileInfo.withAccessKey(IDGenerator.MD5.generate());
                fileInfo.setMd5(ByteBufUtil.hexDump(md5.digest()));
                fileInfo.setSha256(ByteBufUtil.hexDump(sha256.digest()));
                fileInfo.setLength(savedFile.length());
                fileInfo.setCreateTime(System.currentTimeMillis());
                fileInfo.setOptions(options);
                FileEntity entity = FileEntity.of(fileInfo, storagePath, serverNodeId);
                return repository
                    .insert(entity)
                    .then(Mono.fromSupplier(entity::toInfo));
            }));
    }

    @Override
    public Mono<FileInfo> saveFile(String name, Flux<DataBuffer> stream, FileOption... options) {
        return doSaveFile(name, stream, options);
    }

    @Override
    public Mono<FileInfo> getFileByMd5(String md5) {
        return repository
            .createQuery()
            .where(FileEntity::getMd5, md5)
            .fetchOne()
            .map(FileEntity::toInfo);
    }

    @Override
    public Mono<FileInfo> getFileBySha256(String sha256) {
        return repository
            .createQuery()
            .where(FileEntity::getSha256, sha256)
            .fetchOne()
            .map(FileEntity::toInfo);
    }

    @Override
    public Mono<FileInfo> getFile(String id) {
        return repository
            .findById(id)
            .map(FileEntity::toInfo);
    }

    private Flux<DataBuffer> readFile(String filePath, long position) {
        return DataBufferUtils
            .read(new FileSystemResource(Paths.get(properties.getStorageBasePath(), filePath)),
                  position,
                  bufferFactory,
                  (int) properties.getReadBufferSize().toBytes())
            .onErrorMap(NoSuchFileException.class, e -> new NotFoundException());
    }

    private Flux<DataBuffer> readFile(FileEntity file, long position) {
        if (Objects.equals(file.getServerNodeId(), rpcManager.currentServerId())) {
            return readFile(file.getStoragePath(), position);
        }
        return readFromAnotherServer(file, position);
    }

    protected Flux<DataBuffer> readFromAnotherServer(FileEntity file, long position) {

        return rpcManager
            .getService(file.getServerNodeId(), Service.class)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMapMany(service -> service.read(new ReadRequest(file.getId(), position)))
            .<DataBuffer>map(bufferFactory::wrap)
            .doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
    }

    @Override
    public Flux<DataBuffer> read(String id) {
        return read(id, 0);
    }

    @Override
    public Flux<DataBuffer> read(String id, long position) {
        return repository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMapMany(file -> readFile(file, position));
    }

    @Override
    public Flux<DataBuffer> read(String id, Function<ReaderContext, Mono<Void>> beforeRead) {
        return repository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMapMany(file -> {
                DefaultReaderContext context = new DefaultReaderContext(file.toInfo(), 0);
                return beforeRead
                    .apply(context)
                    .thenMany(Flux.defer(() -> readFile(file, context.position)));
            });
    }

    @AllArgsConstructor
    private static class DefaultReaderContext implements ReaderContext {
        private final FileInfo info;
        private long position;

        @Override
        public FileInfo info() {
            return info;
        }

        @Override
        public void position(long position) {
            this.position = position;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReadRequest {
        private String id;
        private long position;
    }

    @io.scalecube.services.annotations.Service
    public interface Service {

        @ServiceMethod
        Flux<ByteBuf> read(ReadRequest request);
    }


    public class ServiceImpl implements Service {
        @Override
        public Flux<ByteBuf> read(ReadRequest request) {
            return ClusterFileManager
                .this
                .read(request.id, request.position)
                .map(buf -> {
                    if (buf instanceof NettyDataBuffer) {
                        return ((NettyDataBuffer) buf).getNativeBuffer();
                    }
                    return Unpooled.wrappedBuffer(buf.asByteBuffer());
                });
        }
    }
}
