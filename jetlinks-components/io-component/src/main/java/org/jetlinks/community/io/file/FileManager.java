package org.jetlinks.community.io.file;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 文件管理器,统一管理文件信息
 */
public interface FileManager {

    Mono<FileInfo> saveFile(FilePart filePart, FileOption... options);

    Mono<FileInfo> saveFile(String name, Flux<DataBuffer> stream, FileOption... options);

    Mono<FileInfo> getFile(String id);

    Mono<FileInfo> getFileByMd5(String md5);

    Mono<FileInfo> getFileBySha256(String sha256);

    Flux<DataBuffer> read(String id);

    Flux<DataBuffer> read(String id, long position);

    Flux<DataBuffer> read(String id,
                          Function<ReaderContext, Mono<Void>> beforeRead);

    Mono<Integer> delete(String id);

    interface ReaderContext {
        FileInfo info();

        void position(long position);
    }
}
