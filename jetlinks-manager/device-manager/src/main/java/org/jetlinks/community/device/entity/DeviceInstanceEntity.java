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
import org.apache.commons.collections.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceInfo;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.MergeOption;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.enums.DeviceFeature;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Index;
import javax.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Setter
@Table(name = "dev_device_instance", indexes = {
    @Index(name = "idx_dev_product_id", columnList = "product_id"),
    @Index(name = "idx_dev_parent_id", columnList = "parent_id"),
    @Index(name = "idx_dev_state", columnList = "state")
})
@Comment("设备信息表")
@EnableEntityEvent
public class DeviceInstanceEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @Schema(description = "设备ID(只能由数字,字母,下划线和中划线组成)")
    public String getId() {
        return super.getId();
    }

    @Column(name = "photo_url", length = 2048)
    @Schema(description = "图片地址")
    private String photoUrl;

    @Column(name = "name")
    @NotBlank(message = "设备名称不能为空", groups = CreateGroup.class)
    @Schema(description = "设备名称")
    private String name;

    @Column
    @ColumnType(javaType = String.class)
    @EnumCodec
    @Schema(description = "设备类型")
    private DeviceType deviceType;

    @Comment("说明")
    @Column(name = "describe")
    @Schema(description = "说明")
    private String describe;

    @Column(name = "product_id", length = 64, updatable = false)
    @NotBlank(message = "产品ID不能为空", groups = CreateGroup.class)
    @Schema(description = "产品ID")
    private String productId;

    @Column(name = "product_name")
    @NotBlank(message = "产品名称不能为空", groups = CreateGroup.class)
    @Schema(description = "产品名称")
    private String productName;

    @Column(name = "configuration")
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @JsonCodec
    @Schema(description = "配置信息")
    private Map<String, Object> configuration;

    @Column(name = "derive_metadata")
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "派生(独立)物模型")
    private String deriveMetadata;

    @Column(name = "state", length = 16)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("notActive")
    @Schema(
        description = "状态(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
        , defaultValue = "notActive"
    )
    private DeviceState state;

    @Column(name = "creator_id", updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(name = "creator_name", updatable = false)
    @Schema(
        description = "创建者名称(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorName;

    @Column(name = "create_time", updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Column(name = "registry_time")
    @Schema(description = "激活时间"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long registryTime;

    @Column(name = "org_id", length = 64)
    @Schema(description = "机构ID", hidden = true)
    //已弃用,机构和设备存在多对多关系,已由资产功能统一管理
    @Deprecated
    private String orgId;

    @Column(name = "parent_id", length = 64)
    @Schema(description = "父级设备ID")
    private String parentId;

    //拓展特性,比如是否为子设备独立状态管理。
    @Column
    @ColumnType(javaType = Long.class, jdbcType = JDBCType.BIGINT)
    @EnumCodec(toMask = true)
    @Schema(description = "设备特性")
    @DefaultValue("0")
    private DeviceFeature[] features;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "修改时间"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long modifyTime;

    @Column(length = 64)
    @Schema(
        description = "修改人ID"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String modifierId;

    @Column(length = 64)
    @Schema(
        description = "修改人名称"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String modifierName;

    public Optional<Object> getConfiguration(String key) {
        if (configuration == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(configuration.get(key));
    }

    public <T> Optional<T> getConfiguration(ConfigKey<T> key) {
        return this
            .getConfiguration(key.getKey())
            .map(key::convertValue);
    }

    public DeviceInfo toDeviceInfo(boolean includeConfiguration) {
        DeviceInfo info = DeviceInfo
            .builder()
            .id(this.getId())
            .productId(this.getProductId())
            .metadata(this.getDeriveMetadata())
            .build();

        if (!includeConfiguration) {
            return info;
        }

        if (!CollectionUtils.isEmpty(configuration)) {
            info.addConfigs(configuration);
        }
        info.addConfig(DeviceConfigKey.parentGatewayId, this.getParentId());
        info.addConfig(PropertyConstants.deviceName, name);
        info.addConfig(PropertyConstants.productName, productName);
        info.addConfig(PropertyConstants.creatorId, creatorId);
        if (hasFeature(DeviceFeature.selfManageState)) {
            info.addConfig(DeviceConfigKey.selfManageState, true);
        }

        return info;
    }

    public DeviceInfo toDeviceInfo() {
        return toDeviceInfo(true);
    }

    public void mergeConfiguration(Map<String, Object> configuration, boolean ignoreExists) {
        if (this.configuration == null) {
            this.configuration = new HashMap<>();
        }
        if (MapUtils.isEmpty(configuration)) {
            return;
        }
        Map<String, Object> newConf = new HashMap<>(configuration);
        //状态自管理，单独设置到feature中
        Object selfManageState = newConf.remove(DeviceConfigKey.selfManageState.getKey());
        if (null != selfManageState) {
            if (Boolean.TRUE.equals(selfManageState)) {
                addFeature(DeviceFeature.selfManageState);
            } else {
                removeFeature(DeviceFeature.selfManageState);
            }
        }
        //物模型单独设置
        Object metadata = newConf.remove(DeviceConfigKey.metadata.getKey());
        if (null != metadata) {
            setDeriveMetadata(String.valueOf(metadata));
        }
        if (ignoreExists) {
            newConf.forEach(this.configuration::putIfAbsent);
        } else {
            this.configuration.putAll(newConf);
        }
    }

    public void mergeConfiguration(Map<String, Object> configuration) {
        mergeConfiguration(configuration, false);
    }

    public Mono<String> mergeMetadata(String metadata) {
        return JetLinksDeviceMetadataCodec
            .getInstance()
            .decode(metadata)
            .flatMap(this::mergeMetadata);
    }

    public Mono<String> mergeMetadata(DeviceMetadata metadata) {
        JetLinksDeviceMetadataCodec codec = JetLinksDeviceMetadataCodec.getInstance();

        if (ObjectUtils.isEmpty(this.getDeriveMetadata())) {
            return codec.encode(metadata)
                        .doOnNext(this::setDeriveMetadata);
        }

        return Mono
            .zip(
                codec.decode(getDeriveMetadata()),
                Mono.just(metadata),
                (derive, another) -> derive.merge(another, MergeOption.ignoreExists)
            )
            .flatMap(codec::encode)
            .doOnNext(this::setDeriveMetadata);
    }

    public void addFeature(DeviceFeature... features) {
        if (this.features == null) {
            this.features = features;
        }
        else if (features.length > 0) {
            this.features = Stream
                .concat(Stream.of(this.features), Stream.of(features))
                .toArray(DeviceFeature[]::new);
        }
    }

    public void removeFeature(DeviceFeature... features) {
        if (this.features != null) {
            List<DeviceFeature> featureList = new ArrayList<>(Arrays.asList(this.features));
            for (DeviceFeature feature : features) {
                featureList.remove(feature);
            }
            this.features = featureList.toArray(new DeviceFeature[0]);
        }
    }


    public boolean hasFeature(DeviceFeature feature) {
        if (this.features == null) {
            return false;
        }
        return EnumDict.in(feature, this.features);
    }

    public void validateId() {
        tryValidate(DeviceInstanceEntity::getId, CreateGroup.class);
    }

    public static DeviceInstanceEntity of(){
        return EntityFactoryHolder.newInstance(DeviceInstanceEntity.class,DeviceInstanceEntity::new);
    }

    public static List<PropertyMetadata> createMetadata(){
        return Arrays.asList(
            SimplePropertyMetadata.of("id", "设备id", StringType.GLOBAL),
            SimplePropertyMetadata.of("name", "设备名称", StringType.GLOBAL),
            SimplePropertyMetadata.of("deviceType", "设备类型", new EnumType()
                .addElement(EnumType.Element.of("device", "直连设备"))
                .addElement(EnumType.Element.of("childrenDevice", "网关子设备"))
                .addElement(EnumType.Element.of("gateway", "网关设备"))),
            SimplePropertyMetadata.of("describe", "说明", StringType.GLOBAL),
            SimplePropertyMetadata.of("productId", "产品id", StringType.GLOBAL),
            SimplePropertyMetadata.of("productName", "产品名称", StringType.GLOBAL),
            SimplePropertyMetadata.of("configuration", "配置", new ObjectType()),
            SimplePropertyMetadata.of("state", "设备状态", new EnumType()
                .addElement(EnumType.Element.of("notActive", "禁用"))
                .addElement(EnumType.Element.of("offline", "离线"))
                .addElement(EnumType.Element.of("online", "在线"))),
            SimplePropertyMetadata.of("orgId", "机构id", StringType.GLOBAL),
            SimplePropertyMetadata.of("parentId", "父设备id", StringType.GLOBAL),
            SimplePropertyMetadata.of("deriveMetadata", "独立物模型", StringType.GLOBAL)
        );
    }
}
