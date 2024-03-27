package org.jetlinks.community.io.file;

import lombok.AllArgsConstructor;
import org.hswebframework.web.file.service.FileStorageService;
import org.hswebframework.web.id.IDGenerator;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;

@AllArgsConstructor
public class FileManagerStorageService implements FileStorageService {

    private final FileManager fileManager;

    @Override
    public Mono<String> saveFile(FilePart filePart) {

        return fileManager
            .saveFile(filePart, FileOption.publicAccess)
            .map(FileInfo::getAccessUrl);
    }

    @Override
    public Mono<String> saveFile(InputStream inputStream, String fileType) {
        return fileManager
            .saveFile(IDGenerator.RANDOM.generate() + "." + fileType,
                      DataBufferUtils
                          .readInputStream(
                              () -> inputStream,
                              DefaultDataBufferFactory.sharedInstance,
                              64 * 1024)
                          .subscribeOn(Schedulers.boundedElastic()),
                      FileOption.publicAccess)
            .map(FileInfo::getAccessUrl);
    }
}
