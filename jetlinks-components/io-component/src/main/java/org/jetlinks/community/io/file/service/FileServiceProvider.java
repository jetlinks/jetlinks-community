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

import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.file.FileOption;
import org.jetlinks.community.io.file.info.ShardingUploadResult;
import org.jetlinks.community.io.file.info.UploadResponse;
import org.jetlinks.community.spi.Provider;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 文件服务供应商
 *
 * @author gyl
 * @since 2.3
 */
public interface FileServiceProvider extends Ordered {

    Provider<FileServiceProvider> providers = Provider.create(FileServiceProvider.class);

    String getType();

    /**
     * 保存文件片段,用于分片上传文件.
     * <p>
     * 注意：在全部分片上传完成后. {@link FileInfo#getAccessUrl()}才会有值.
     *
     * @param sessionId   会话ID,相同的会话ID的文件分片将被保存为同一个文件.
     * @param fileId
     * @param storagePath 文件相对路径
     * @param length      文件总长度
     * @param offset      当前分片的偏移量
     * @param stream      文件流
     * @param options
     * @return 保存响应
     */
    Mono<ShardingUploadResult> saveFile(String sessionId, String fileId, String storagePath, long length, long offset, Flux<DataBuffer> stream, FileOption... options);

    /**
     * 保存文件
     *
     * @param storagePath 文件路径
     * @param filePart    文件内容
     * @param options
     * @return 文件信息
     */
    default Mono<UploadResponse> saveFile(String storagePath, FilePart filePart, FileOption... options) {
        return saveFile(storagePath, filePart.content(), options);
    }

    /**
     * 保存文件
     *
     * @param storagePath 文件路径
     * @param stream      文件内容
     * @param options
     * @return 文件信息
     */
    Mono<UploadResponse> saveFile(String storagePath, Flux<DataBuffer> stream, FileOption... options);

    /**
     * 读取文件
     *
     * @param fileInfo   文件内容
     * @param beforeRead 在读取前执行
     * @return 文件流
     */
    Flux<DataBuffer> read(FileInfo fileInfo, Function<FileManager.ReaderContext, Mono<Void>> beforeRead);


    /**
     * 删除文件
     *
     * @param storagePath 文件相对路径
     */
    Mono<Void> delete(String storagePath);


    default int getOrder() {
        return 0;
    }
}
