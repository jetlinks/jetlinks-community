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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "s_file")
@EnableEntityEvent
public class FileEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column(nullable = false)
    @Schema(description = "文件名称")
    @NotNull
    private String name;

    @Column(nullable = false)
    @Schema(description = "文件类型")
    private String extension;

    @Column(nullable = false, updatable = false)
    @Schema(description = "文件大小")
    private Long length;

    @Column(nullable = false, updatable = false, length = 32)
    @Schema(description = "MD5哈希值")
    private String md5;

    @Column(nullable = false, updatable = false, length = 64)
    @Schema(description = "SHA-256哈希值")
    private String sha256;

    @Column
    @Schema(description = "过期时间(毫秒时间戳)")
    private Long expires;

    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Column(length = 64, updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(length = 64, nullable = false)
    @Schema(description = "集群节点ID")
    @DefaultValue("default")
    private String serverNodeId;

    @Column(length = 512, updatable = false, nullable = false)
    @Schema(description = "存储地址")
    @NotNull
    private String storagePath;

    @Column
    @EnumCodec(toMask = true)
    @ColumnType(jdbcType = JDBCType.BIGINT, javaType = Long.class)
    @Schema(description = "扩展信息")
    private FileOption[] options;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @Schema(description = "其他配置")
    private Map<String, Object> others;


    public FileInfo toInfo() {
        FileInfo fileInfo = copyTo(new FileInfo());
        fileInfo.setPath(storagePath);
        return fileInfo;
    }

    public static FileEntity of(FileInfo fileInfo, String storagePath) {
        FileEntity fileEntity = new FileEntity().copyFrom(fileInfo);
        fileEntity.setStoragePath(storagePath);

        return fileEntity;
    }

}
