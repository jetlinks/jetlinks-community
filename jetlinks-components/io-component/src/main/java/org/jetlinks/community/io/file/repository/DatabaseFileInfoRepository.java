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

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.io.file.FileEntity;
import org.jetlinks.community.io.file.FileEntityService;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author gyl
 * @since 2.3
 */
@AllArgsConstructor
public class DatabaseFileInfoRepository implements FileInfoRepository {

    public static final String TYPE = "database";
    private final FileEntityService service;


    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isSupported(String id) {
        return true;
    }

    @Override
    public Mono<FileInfo> saveFileInfo(FileInfo fileInfo) {
        if (StringUtils.isBlank(fileInfo.getId())) {
            fileInfo.setId(IDGenerator.RANDOM.generate());
        }
        FileEntity entity = FileEntity.of(fileInfo, fileInfo.getPath());
        fileInfo
            .expirationTime()
            .ifPresent(entity::setExpires);
        ValidatorUtils.tryValidate(entity);
        return service
            .save(entity)
            .thenReturn(fileInfo);
    }

    @Override
    public Mono<FileInfo> prepare(FileInfo fileInfo) {
        return Mono.fromCallable(() -> {
            if (StringUtils.isBlank(fileInfo.getId())) {
                fileInfo.setId(IDGenerator.RANDOM.generate());
            }
            return fileInfo;
        });
    }

    @Override
    public Mono<FileInfo> getFileInfo(String id) {
        return service
            .findById(id)
            .map(FileEntity::toInfo);
    }

    @Override
    public Mono<FileInfo> getFileInfoByMd5(String md5) {
        return service
            .createQuery()
            .where(FileEntity::getMd5, md5)
            .fetchOne()
            .map(FileEntity::toInfo);
    }

    @Override
    public Mono<FileInfo> getFileInfoBySha256(String sha256) {
        return service
            .createQuery()
            .where(FileEntity::getSha256, sha256)
            .fetchOne()
            .map(FileEntity::toInfo);
    }

    @Override
    public Mono<FileInfo> deleteFileInfo(String id) {
        return getFileInfo(id)
            .flatMap(info -> service
                .createDelete()
                .where(FileEntity::getId, id)
                .execute()
                .thenReturn(info));
    }

    @Override
    public Flux<FileInfo> deleteExpiredFileInfo(long before, Duration tempFilePeriod) {
        return service
            .createQuery()
            .lte(FileEntity::getExpires, before)
            .orNest()
            //查询超过时间的暂存文件
            .when(tempFilePeriod.toMillis() != 0, q -> q
                .lte(FileEntity::getCreateTime,
                     before - tempFilePeriod.toMillis())
                .and(FileEntity::getOptions, "in$any", FileOption.tempFile))
            .end()
            .fetch()
            .collectMap(GenericEntity::getId, FileEntity::toInfo)
            .flatMapMany(infos -> service
                .deleteById(Flux.fromIterable(infos.keySet()))
                //不发出事件
                .as(EntityEventHelper::setDoNotFireEvent)
                .thenMany(Flux.fromIterable(infos.values())));
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
