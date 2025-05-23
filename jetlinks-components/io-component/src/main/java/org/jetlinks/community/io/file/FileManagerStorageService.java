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
