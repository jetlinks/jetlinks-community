package org.jetlinks.community.io.file;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;


public interface FileManager {

    Mono<FileInfo> saveFile(FilePart filePart);

    Mono<FileInfo> saveFile(String name, Flux<DataBuffer> stream);

    Mono<FileInfo> getFile(String id);

    Flux<DataBuffer> read(String id);

    Flux<DataBuffer> read(String id, long position);

    Flux<DataBuffer> read(String id,
                          Function<ReaderContext,Mono<Void>> beforeRead);

    interface ReaderContext{
        FileInfo info();

        void position(long position);
    }
}
