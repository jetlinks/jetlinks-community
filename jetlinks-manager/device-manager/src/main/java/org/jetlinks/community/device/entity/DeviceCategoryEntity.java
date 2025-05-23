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
package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.i18n.MultipleI18nSupportEntity;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_product_category")
@Comment("产品分类信息表")
@EnableEntityEvent
public class DeviceCategoryEntity extends GenericTreeSortSupportEntity<String> implements RecordCreationEntity, MultipleI18nSupportEntity {

    @Override
    @Id
    @Column(length = 64, updatable = false)
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @NotBlank(message = "ID不能为空", groups = CreateGroup.class)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-|]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    public String getId() {
        return super.getId();
    }

    @Schema(description = "标识")
    @Column(nullable = false,length = 64)
    @NotBlank(message = "标识不能为空", groups = CreateGroup.class)
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "分类标识只能由数字,字母,下划线和中划线组成")
    private String key;

    @Schema(description = "名称")
    @Column(nullable = false)
    @NotBlank
    private String name;

    @Schema(description = "说明")
    @Column
    private String description;

    @Schema(description = "子节点")
    private List<DeviceCategoryEntity> children;

    @Schema(description = "物模型")
    @Column
    @ColumnType(javaType = String.class, jdbcType = JDBCType.CLOB)
    private String metadata;

    @Column(updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Schema(title = "国际化信息定义")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private Map<String, Map<String, String>> i18nMessages;

    public String getI18nName() {
        return getI18nMessage("name", name);
    }
}
