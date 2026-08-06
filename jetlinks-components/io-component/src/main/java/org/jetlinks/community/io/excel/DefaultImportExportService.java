/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.io.excel;

import org.hswebframework.reactor.excel.converter.RowWrapper;
import org.hswebframework.utils.StringUtils;
import org.jetlinks.community.io.excel.easyexcel.ExcelReadDataListener;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.utils.FileUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.URI;

import static org.hswebframework.reactor.excel.ReactorExcel.read;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class DefaultImportExportService implements ImportExportService {

    private final FileManager fileManager;

    public DefaultImportExportService(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public <T> Flux<RowResult<T>> doImport(Class<T> clazz, String fileUrl) {
        return getInputStream(fileUrl)
            .flatMapMany(inputStream -> ExcelReadDataListener.of(inputStream, clazz));
    }

    @Override
    public <T> Flux<RowResult<T>> doImport(Class<T> clazz, InputStream stream) {
        return ExcelReadDataListener.of(stream, clazz);
    }


    @Override
    public <T> Flux<T> readData(String fileUrl, String fileId, RowWrapper<T> wrapper) {
        if (!StringUtils.isNullOrEmpty(fileUrl)) {
            return getInputStream(fileUrl)
                .flatMapMany(inputStream -> read(inputStream, FileUtils.getExtension(fileUrl), wrapper));
        } else {

            return Mono
                .zip(fileManager
                         .read(fileId)
                         .as(DataBufferUtils::join)
                         .map(buffer->buffer.asInputStream(true)),
                     fileManager.getFile(fileId))
                .flatMapMany(t2 -> read(t2.getT1(), t2.getT2().getExtension(), wrapper));
        }
    }

    public Mono<InputStream> getInputStream(String fileUrl) {
        // 导入入口只接受平台托管文件 ID，避免由请求参数触发任意网络或本地文件访问。
        return fileManager
            .read(resolveFileId(fileUrl))
            .as(DataBufferUtils::join)
            .map(buffer -> buffer.asInputStream(true));
    }

    static String resolveFileId(String fileUrl) {
        URI uri = URI.create(fileUrl);
        if (!uri.isAbsolute()) {
            if (fileUrl.contains("/") || fileUrl.contains("\\")) {
                throw new IllegalArgumentException("Only managed file IDs are supported");
            }
            return fileUrl;
        }

        String path = uri.getPath();
        int filePathIndex = path == null ? -1 : path.lastIndexOf("/file/");
        if (filePathIndex < 0) {
            throw new IllegalArgumentException("Only managed file URLs are supported");
        }
        String fileName = path.substring(filePathIndex + "/file/".length());
        int extensionIndex = fileName.indexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }
}
