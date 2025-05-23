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

import org.jetlinks.sdk.server.file.PrepareUploadRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 文件管理器,统一管理文件信息
 *
 * @author zhouhao
 * @see FileCommandSupportManager
 * @since 2.0
 */
public interface FileManager {


    /**
     * 预存储文件信息，在文件保存前就存储相关信息
     *
     * @param request 文件信息
     * @param options 保存选项
     * @return 预存储后文件信息
     */
    default Mono<FileInfo> prepareUpload(PrepareUploadRequest request, FileOption... options) {
        return Mono.error(UnsupportedOperationException::new);
    }

    /**
     * 保存文件片段,用于分片上传文件.
     * <p>
     * 注意：在全部分片上传完成后. {@link FileInfo#getAccessUrl()}才会有值.
     *
     * @param sessionId 会话ID,相同的会话ID的文件分片将被保存为同一个文件.
     * @param name      文件名
     * @param length    文件总长度
     * @param offset    当前分片的偏移量
     * @param stream    文件流
     * @param options   保存选项
     * @return 文件信息
     * @since 2.2
     */
    Mono<FileInfo> saveFile(String sessionId,
                            String name,
                            long length,
                            long offset,
                            Flux<DataBuffer> stream,
                            FileOption... options);

    /**
     * 保存文件,通常用于使用WebFlux上传文件
     *
     * @param filePart FilePart
     * @param options  文件选型
     * @return 文件信息
     */
    Mono<FileInfo> saveFile(FilePart filePart, FileOption... options);

    /**
     * 保存文件,通常用于使用自定义文件内容上传文件
     *
     * @param name    文件名
     * @param stream  文件内容
     * @param options 文件选项
     * @return 文件信息
     */
    Mono<FileInfo> saveFile(String name, Flux<DataBuffer> stream, FileOption... options);


    /**
     * 保存文件片段,用于基于已预存的文件信息分片上传文件.
     * <p>
     * 注意：在全部分片上传完成后. {@link FileInfo#getAccessUrl()}才会有值.
     *
     * @param fileId  文件id
     * @param offset  当前分片的偏移量
     * @param stream  文件流
     * @return 文件信息
     * @since 2.2
     */
    default Mono<FileInfo> saveFileById(String fileId,
                                        long length,
                                        long offset,
                                        Flux<DataBuffer> stream) {
        return Mono.error(UnsupportedOperationException::new);
    }

    /**
     * 基于已预存的文件信息保存文件
     *
     * @param fileId 已预存的文件id
     * @param stream 文件内容
     * @return 文件信息
     */
    default Mono<FileInfo> saveFileById(String fileId, Flux<DataBuffer> stream) {
        return Mono.error(UnsupportedOperationException::new);
    }

    /**
     * 获取文件信息
     *
     * @param id ID
     * @return 文件信息
     */
    Mono<FileInfo> getFile(String id);

    /**
     * 根据文件的MD5值获取文件
     *
     * @param md5 md5
     * @return 文件信息
     */
    Mono<FileInfo> getFileByMd5(String md5);

    /**
     * 根据文件的SHA256值获取文件
     *
     * @param sha256 sha256
     * @return 文件信息
     */
    Mono<FileInfo> getFileBySha256(String sha256);

    /**
     * 根据文件ID读取文件
     *
     * @param id ID {@link FileInfo#getId()}
     * @return 文件流
     */
    Flux<DataBuffer> read(String id);

    /**
     * 根据文件ID读取文件
     *
     * @param id       ID {@link FileInfo#getId()}
     * @param position 读取位置
     * @return 文件流
     */
    Flux<DataBuffer> read(String id, long position);

    /**
     * 根据文件ID读取文件
     *
     * @param id         ID {@link FileInfo#getId()}
     * @param beforeRead 在读取前执行
     * @return 文件流
     */
    Flux<DataBuffer> read(String id,
                          Function<ReaderContext, Mono<Void>> beforeRead);

    /**
     * 根据文件ID删除文件
     *
     * @param id         ID {@link FileInfo#getId()}
     */
    Mono<Void> delete(String id);


    interface ReaderContext {
        /**
         * @return 文件信息
         */
        FileInfo info();

        /**
         * 设置读取开始位置
         *
         * @param position 读取开始位置
         */
        void position(long position);

        /**
         * 读取长度
         *
         * @param length 长度
         */
        void length(long length);
    }
}
