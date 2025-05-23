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
package org.jetlinks.community.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import jakarta.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_protocol")
@Comment("协议信息表")
@EnableEntityEvent
public class ProtocolSupportEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column
    @Schema(description = "协议名称")
    private String name;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column
    @Schema(description = "类型")
    private String type;

    @Column
    @Schema(description = "状态，1启用，0禁用")
    @DefaultValue("1")
    private Byte state;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "配置")
    private Map<String, Object> configuration;

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

    @Column(name = "creator_name", updatable = false)
    @Schema(
        description = "创建者名称(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorName;

    @Column(length = 64)
    @Schema(description = "修改人")
    private String modifierId;

    @Column
    @Schema(description = "修改时间")
    private Long modifyTime;

    @Column(length = 64)
    @Schema(description = "修改人名称")
    private String modifierName;

    public ProtocolSupportDefinition toUnDeployDefinition() {
        ProtocolSupportDefinition definition = toDeployDefinition();
        definition.setState((byte) 0);
        return definition;
    }

    public ProtocolSupportDefinition toDeployDefinition() {
        ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
        definition.setId(getId());
        definition.setConfiguration(configuration);
        definition.setName(name);
        definition.setDescription(description);
        definition.setProvider(type);
        definition.setState((byte) 1);

        return definition;
    }

    public ProtocolSupportDefinition toDefinition() {
        ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
        definition.setId(getId());
        definition.setConfiguration(configuration);
        definition.setName(name);
        definition.setDescription(description);
        definition.setProvider(type);
        definition.setState(getState());
        return definition;
    }


    public static List<PropertyMetadata> createMetadata(){
        return Arrays.asList(
            SimplePropertyMetadata.of("id", "协议id", StringType.GLOBAL),
            SimplePropertyMetadata.of("name", "协议名称", StringType.GLOBAL),
            SimplePropertyMetadata.of("type", "协议类型", StringType.GLOBAL),
            SimplePropertyMetadata.of("configuration", "配置", new ObjectType()),
            SimplePropertyMetadata.of("describe", "说明", StringType.GLOBAL),
            SimplePropertyMetadata.of("state", "状态", new EnumType()
                .addElement(EnumType.Element.of("0", "禁用"))
                .addElement(EnumType.Element.of("1", "离线")))
        );
    }
}
