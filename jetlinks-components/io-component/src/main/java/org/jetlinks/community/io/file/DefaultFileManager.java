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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.config.ConfigManager;
import org.jetlinks.community.io.FileConstants;
import org.jetlinks.community.io.file.repository.FileInfoRepository;
import org.jetlinks.community.io.file.repository.FileInfoRepositoryHelper;
import org.jetlinks.community.io.file.service.FileServiceHelper;
import org.jetlinks.community.io.file.service.FileServiceProvider;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.sdk.server.file.PrepareUploadRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.Assert;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author gyl
 * @since 2.3
 */
@Slf4j
public class DefaultFileManager implements FileManager {

    /**
     * <pre>{@code
     * system:
     *   config:
     *     scopes:
     *       - id: paths
     *         name: 访问路径配置
     *         public-access: true
     *         properties:
     *           - key: base-path
     *             name: 接口根路径
     *             default-value: ${api.base-path}
     * }</pre>
     */
    public static final String API_PATH_CONFIG_NAME = "paths";
    public static final String API_PATH_CONFIG_KEY = "base-path";

    private final FileProperties fileProperties;

    private final ConfigManager configManager;
    private final FileInfoRepositoryHelper fileInfoRepositoryHelper;

    public DefaultFileManager(FileProperties fileProperties, ConfigManager configManager, FileInfoRepositoryHelper fileInfoRepositoryHelper) {
        this.fileProperties = fileProperties;
        this.configManager = configManager;
        this.fileInfoRepositoryHelper = fileInfoRepositoryHelper;
    }

    @Override
    public Mono<FileInfo> prepareUpload(PrepareUploadRequest request, FileOption... options) {
        FileInfoRepository repository = fileInfoRepositoryHelper.getRepository();
        FileInfo fileInfo = prepareToInfo(request);
        fileInfo.setOptions(options);
        return prepare(repository, fileInfo)
            .flatMap(this::fillExistsInfo)
            .flatMap(repository::saveFileInfo);
    }


    private static FileInfo prepareToInfo(PrepareUploadRequest request) {
        FileInfo fileInfo = new FileInfo();
        FastBeanCopier.copy(request, fileInfo);
        fileInfo.withFileName(request.getName());
        return fileInfo;
    }

    @Override
    public Mono<FileInfo> saveFile(String sessionId,
                                   String name,
                                   long length,
                                   long offset,
                                   Flux<DataBuffer> stream,
                                   FileOption... options) {
        FileInfoRepository repository = fileInfoRepositoryHelper.getRepository();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOptions(options);
        fileInfo.withFileName(name);
        fileInfo.setLength(length);
        return prepare(repository, fileInfo)
            .flatMap(info -> getServiceFromInfo(info)
                .saveFile(sessionId, info.getId(), info.getPath(), length, offset, stream, options)
                .flatMap(r -> {
                    FileInfo _info = r.getResponse().toShardingInfo(info);
                    _info.setId(r.getFileId());
                    if (r.isComplete()) {
                        //完成时存储文件信息
                        return fillExistsInfo(_info)
                            .flatMap(repository::saveFileInfo);
                    }
                    return Mono.just(_info);
                }));
    }

    @Override
    public Mono<FileInfo> saveFile(FilePart filePart, FileOption... options) {
        FileInfoRepository repository = fileInfoRepositoryHelper.getRepository();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOptions(options);
        fileInfo.withFileName(filePart.filename());
        return prepare(repository, fileInfo)
            .flatMap(info -> getServiceFromInfo(info)
                .saveFile(info.getPath(), filePart, options)
                .flatMap(res -> fillExistsInfo(res.toInfo(info)))
                .flatMap(repository::saveFileInfo));
    }

    @Override
    public Mono<FileInfo> saveFile(String name, Flux<DataBuffer> stream, FileOption... options) {
        FileInfoRepository repository = fileInfoRepositoryHelper.getRepository();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOptions(options);
        fileInfo.withFileName(name);
        return prepare(repository, fileInfo)
            .flatMap(info -> getServiceFromInfo(info)
                .saveFile(info.getPath(), stream, options)
                .flatMap(res -> fillExistsInfo(res.toInfo(info)))
                .flatMap(repository::saveFileInfo));
    }


    private Mono<FileInfo> prepare(FileInfoRepository repository, FileInfo fileInfo) {
        return Mono
            .deferContextual(ctx -> {
                ctx.getOrEmpty(FileConstants.ContextKey.expiration.getKey())
                   .ifPresent(expirationTime -> {
                       //填充上下文中的过期时间
                       fileInfo.withOther(FileConstants.ContextKey.expiration.getKey(), expirationTime);
                   });
                //填充本次指定文件服务
                fileInfo.withOther(FileConstants.ContextKey.fileService.getKey(), FileServiceHelper.getFileService(ctx, fileProperties.getDefaultService()));
                return repository
                    .prepare(fileInfo)
                    .map(DefaultFileManager::prepare);
            });
    }

    /**
     * 填充访问密钥,创建时间,文件相对路径
     * <pre>
     * 文件相对路径:yyyyMMdd/id.yy,即 文件创建时间/文件id.文件后缀
     * </pre>
     */
    private static FileInfo prepare(FileInfo fileInfo) {
        if (!fileInfo.accessKey().isPresent()) {
            fileInfo.withAccessKey(IDGenerator.MD5.generate());
        }
        if (fileInfo.getCreateTime() == 0) {
            fileInfo.setCreateTime(System.currentTimeMillis());
        }
        if (StringUtils.isBlank(fileInfo.getPath())) {
            fileInfo.setPath(generateFilePath(fileInfo).toString());
        }
        return fileInfo;
    }

    private static Path generateFilePath(FileInfo fileInfo) {
        Assert.hasText(fileInfo.getId(), "error.file_id_can_not_be_empty");
        String extension = fileInfo.getExtension();
        String _extension = StringUtils.isNotBlank(extension) ? "." + extension : StringUtils.EMPTY;
        String time = CastUtils
            .castLocalDateTime(fileInfo.getCreateTime())
            .format(DateTimeFormatter.BASIC_ISO_DATE);
        return Paths.get(time, fileInfo.getId() + _extension);
    }

    @Override
    public Mono<FileInfo> saveFileById(String fileId,
                                       long length,
                                       long offset,
                                       Flux<DataBuffer> stream) {
        return fileInfoRepositoryHelper
            .findRepositorySingle(fileId, r -> r.getFileInfo(fileId))
            .flatMap(tp2 -> {
                FileInfoRepository repository = tp2.getT1();
                FileInfo info = tp2.getT2();
                //填充path
                DefaultFileManager.prepare(info);
                //填充长度
                info.setLength(length);
                return getServiceFromInfo(info)
                    .saveFile(fileId, fileId, info.getPath(), length, offset, stream, info.getOptions())
                    .flatMap(r -> {
                        FileInfo fileInfo = r.getResponse().toShardingInfo(info);
                        fileInfo.setId(r.getFileId());
                        if (r.isComplete()) {
                            //完成时校验文件信息
                            return fillExistsInfo(fileInfo)
                                .flatMap(i -> repository
                                    .validateSharding(info, i));
                        }
                        return Mono.just(info);
                    });
            });
    }

    @Override
    public Mono<FileInfo> saveFileById(String fileId, Flux<DataBuffer> stream) {
        return fileInfoRepositoryHelper
            .findFileInfoSingle(fileId, r -> r.validate(fileId, stream))
            .flatMap(info -> {
                //填充path
                DefaultFileManager.prepare(info);
                return getServiceFromInfo(info)
                    .saveFile(info.getPath(), stream)
                    .flatMap(res -> fillExistsInfo(res.toInfo(info)));
            });
    }


    @Override
    public Mono<FileInfo> getFile(String id) {
        return fileInfoRepositoryHelper
            .findFileInfoSingle(id, r -> r.getFileInfo(id))
            .flatMap(this::fillExistsInfo);
    }

    @Override
    public Mono<FileInfo> getFileByMd5(String md5) {
        return fileInfoRepositoryHelper
            .findFileInfoSingle(r -> true, r -> r.getFileInfoByMd5(md5))
            .flatMap(this::fillExistsInfo);
    }

    @Override
    public Mono<FileInfo> getFileBySha256(String sha256) {
        return fileInfoRepositoryHelper
            .findFileInfoSingle(r -> true, r -> r.getFileInfoBySha256(sha256))
            .flatMap(this::fillExistsInfo);
    }

    @Override
    public Flux<DataBuffer> read(String id) {
        return read(id, ctx -> Mono.empty());
    }

    @Override
    public Flux<DataBuffer> read(String id, long position) {
        return read(id, ctx -> {
            ctx.position(position);
            return Mono.empty();
        });
    }

    @Override
    public Flux<DataBuffer> read(String id, Function<ReaderContext, Mono<Void>> beforeRead) {
        return fileInfoRepositoryHelper
            .findFileInfoSingle(id, r -> r.getFileInfo(id))
            .map(DefaultFileManager::prepare)
            .flatMapMany(info -> getServiceFromInfo(info).read(info, beforeRead));
    }

    @Override
    public Mono<Void> delete(String id) {
        return fileInfoRepositoryHelper
            .findFileInfoSingle(id, r -> r.deleteFileInfo(id))
            .flatMap(info -> {
                FileInfo _info = prepare(info);
                return getServiceFromInfo(info)
                    .delete(_info.getPath());
            });
    }


    /**
     * 填充已上传文件的信息,例如访问url
     */
    private Mono<FileInfo> fillExistsInfo(FileInfo fileInfo) {
        if (StringUtils.isBlank(fileInfo.getAccessUrl())) {
            return this
                .getApiBasePath()
                .doOnNext(fileInfo::withBasePath)
                .thenReturn(fileInfo);
        }
        return Mono.just(fileInfo);
    }

    /**
     * 删除过期文件
     */
    protected Flux<Void> deleteExpiredFile(long baseTIme) {
        return fileInfoRepositoryHelper
            .walkRepository(r -> r.deleteExpiredFileInfo(baseTIme, fileProperties.getTempFilePeriod()), e -> {
                log.warn("delete expired file info error", e);
            })
            .flatMap(info -> getServiceFromInfo(info).delete(generateFilePath(info).toString()));
    }

    private Mono<String> getApiBasePath() {
        return configManager
            .getProperties(API_PATH_CONFIG_NAME)
            .mapNotNull(val -> val.getString(API_PATH_CONFIG_KEY, null));
    }


    public FileServiceProvider getServiceFromInfo(FileInfo fileInfo) {
        Object type = Optional
            .ofNullable(fileInfo.getOthers())
            .map(others -> others.get(FileConstants.ContextKey.fileService.getKey()))
            .orElse(FileServiceHelper.getHighOrderServiceType());
        return FileServiceProvider.providers.getNow(String.valueOf(type));
    }

}
