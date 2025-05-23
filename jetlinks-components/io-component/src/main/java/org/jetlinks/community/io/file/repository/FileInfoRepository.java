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
package org.jetlinks.community.io.file.repository;

import io.netty.buffer.ByteBufUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.utils.FileUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author gyl
 * @since 2.3
 */
public interface FileInfoRepository extends Ordered {

    String getType();

    /**
     * 当前策略是否支持该文件id进行读操作
     *
     * @param id 文件id
     * @return 是否支持
     */
    boolean isSupported(String id);

    /**
     * 预处理文件信息，例如在文件保存前就存储相关信息
     *
     * @param fileInfo 文件信息
     * @return 预处理后文件信息
     */
    Mono<FileInfo> prepare(FileInfo fileInfo);

    /**
     * 存储文件信息
     *
     * @param fileInfo 文件信息
     * @return 存储文件信息
     */
    Mono<FileInfo> saveFileInfo(FileInfo fileInfo);

    /**
     * 获取文件信息
     *
     * @param id ID
     * @return 文件信息
     */
    Mono<FileInfo> getFileInfo(String id);

    /**
     * 根据文件的MD5值获取文件
     *
     * @param md5 md5
     * @return 文件信息
     */
    Mono<FileInfo> getFileInfoByMd5(String md5);

    /**
     * 根据文件的SHA256值获取文件
     *
     * @param sha256 sha256
     * @return 文件信息
     */
    Mono<FileInfo> getFileInfoBySha256(String sha256);

    /**
     * 删除文件信息
     *
     * @param id 文件id
     */
    Mono<FileInfo> deleteFileInfo(String id);

    /**
     * 删除过期时间<=指定时间的文件，删除创建时间+临时文件保存时间<=指定时间的临时文件
     *
     * @param before         指定时间(毫秒时间戳)
     * @param tempFilePeriod 临时文件保存时间
     * @return 删除文件的id
     */
    Flux<FileInfo> deleteExpiredFileInfo(long before, Duration tempFilePeriod);

    /**
     * 校验该id对应文件信息是否与文件流符合
     *
     * @param id         文件ID
     * @param fileStream 文件流
     * @return 符合则返回文件信息, 否则抛出{@link IllegalArgumentException}.
     */
    default Mono<FileInfo> validate(String id, Flux<DataBuffer> fileStream) {
        //默认对比校验值和长度
        return getFileInfo(id)
            .flatMap(info -> {
                MessageDigest md5 = DigestUtils.getMd5Digest();
                MessageDigest sha256 = DigestUtils.getSha256Digest();
                AtomicLong totalSize = new AtomicLong(0);
                return fileStream
                    .map(f -> FileUtils.updateDigest(md5, FileUtils.updateDigest(sha256, f)))
                    .doOnNext(buffer -> totalSize.addAndGet(buffer.readableByteCount()))
                    .then(Mono.fromCallable(() -> id.equals(info.getId())
                        & info.getLength() == totalSize.get()
                        & ByteBufUtil.hexDump(md5.digest()).equals(info.getMd5())
                        & ByteBufUtil.hexDump(sha256.digest()).equals(info.getSha256())))
                    .flatMap(check -> {
                        if (!check) {
                            return Mono.error(() -> new ValidationException.NoStackTrace("error.unsupported_file"));
                        }
                        return Mono.just(info);
                    });
            });
    }

    default Mono<FileInfo> validateSharding(FileInfo src, FileInfo info) {
        return Mono
            .fromCallable(() -> src.getId().equals(info.getId())
                & src.getPath().equals(info.getPath())
                & src.getLength() == info.getLength()
                & src.getMd5().equals(info.getMd5())
                & src.getSha256().equals(info.getSha256()))
            .flatMap(check -> {
                if (!check) {
                    return Mono.error(() -> new ValidationException.NoStackTrace("error.unsupported_file"));
                }
                return Mono.just(info);
            });
    }
}
