package org.jetlinks.community.io.file;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;


public class DefaultFileManager implements FileManager {

    private final FileProperties properties;

    private final DataBufferFactory bufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    private final ReactiveRepository<FileEntity, String> repository;


    private final WebClient client;

    public DefaultFileManager(WebClient.Builder builder,
                              FileProperties properties,
                              ReactiveRepository<FileEntity, String> repository) {
        new File(properties.getStorageBasePath()).mkdirs();
        this.properties = properties;
        this.client = builder
            .clone()
            .filter(this.properties.createWebClientRute())
            .build();
        this.repository = repository;
    }

    @Override
    public Mono<FileInfo> saveFile(FilePart filePart) {
        return saveFile(filePart.filename(), filePart.content());
    }

    private DataBuffer updateDigest(MessageDigest digest, DataBuffer dataBuffer) {
        dataBuffer = DataBufferUtils.retain(dataBuffer);
        digest.update(dataBuffer.asByteBuffer());
        DataBufferUtils.release(dataBuffer);
        return dataBuffer;
    }

    public Mono<FileInfo> saveFileToCluster(String name, Flux<DataBuffer> stream) {
        String serverId = properties.selectServerNode();
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("file", stream, DataBuffer.class)
               .headers(header -> header
                   .setContentDisposition(ContentDisposition
                                              .builder("form-data")
                                              .name("file")
                                              .filename(name)
                                              .build()))
               .contentType(MediaType.APPLICATION_OCTET_STREAM);
        return client
            .post()
            .uri("http://" + serverId + "/file/" +serverId)
            .attribute(FileProperties.serverNodeIdAttr, serverId)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .retrieve()
            .bodyToMono(FileInfo.class);
    }

    public Mono<FileInfo> doSaveFile(String name, Flux<DataBuffer> stream) {
        LocalDate now = LocalDate.now();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(IDGenerator.MD5.generate());
        fileInfo.withFileName(name);

        String storagePath = now.format(DateTimeFormatter.BASIC_ISO_DATE)
            + "/" + fileInfo.getId() + "." + fileInfo.getExtension();

        MessageDigest md5 = DigestUtils.getMd5Digest();
        MessageDigest sha256 = DigestUtils.getSha256Digest();
        String storageBasePath = properties.getStorageBasePath();
        String serverNodeId = properties.getServerNodeId();
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
                fileInfo.setMd5(ByteBufUtil.hexDump(md5.digest()));
                fileInfo.setSha256(ByteBufUtil.hexDump(sha256.digest()));
                fileInfo.setLength(savedFile.length());
                fileInfo.setCreateTime(System.currentTimeMillis());
                FileEntity entity = FileEntity.of(fileInfo, storagePath, serverNodeId);
                return repository
                    .insert(entity)
                    .then(Mono.fromSupplier(entity::toInfo));
            }));
    }

    @Override
    public Mono<FileInfo> saveFile(String name, Flux<DataBuffer> stream) {
        if (properties.getClusterRute().isEmpty()
            || properties.getClusterRute().containsKey(properties.getServerNodeId())) {
            return doSaveFile(name, stream);
        }
        //配置里集群,但是并不支持本节点,则保存到其他节点
        return saveFileToCluster(name, stream);
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
                  properties.getReadBufferSize());
    }

    private Flux<DataBuffer> readFile(FileEntity file, long position) {
        if (Objects.equals(file.getServerNodeId(), properties.getServerNodeId())) {
            return readFile(file.getStoragePath(), position);
        }
        return readFromAnotherServer(file, position);
    }

    protected Flux<DataBuffer> readFromAnotherServer(FileEntity file, long position) {
        return client
            .get()
            .uri("http://" + file.getServerNodeId() + "/file/{serverNodeId}/{fileId}", file.getServerNodeId(), file.getId())
            .attribute(FileProperties.serverNodeIdAttr, file.getServerNodeId())
            .headers(header -> header.setRange(Collections.singletonList(HttpRange.createByteRange(position))))
            .retrieve()
            .bodyToFlux(DataBuffer.class);
    }

    @Override
    public Flux<DataBuffer> read(String id) {
        return read(id, 0);
    }

    @Override
    public Flux<DataBuffer> read(String id, long position) {
        return repository
            .findById(id)
            .flatMapMany(file -> readFile(file, position));
    }

    @Override
    public Flux<DataBuffer> read(String id, Function<ReaderContext, Mono<Void>> beforeRead) {
        return repository
            .findById(id)
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

}
