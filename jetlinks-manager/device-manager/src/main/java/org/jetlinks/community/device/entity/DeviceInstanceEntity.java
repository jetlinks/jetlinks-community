package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceInfo;
import org.jetlinks.community.device.enums.DeviceState;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_device_instance", indexes = {
    @Index(name = "idx_dev_product_id", columnList = "product_id"),
    @Index(name = "idx_dev_parent_id", columnList = "parent_id"),
    @Index(name = "idx_dev_state", columnList = "state")
})
public class DeviceInstanceEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @Schema(description = "设备ID(只能由数字,字母,下划线和中划线组成)")
    public String getId() {
        return super.getId();
    }

    @Comment("设备实例名称")
    @Column(name = "name")
    @NotBlank(message = "设备名称不能为空", groups = CreateGroup.class)
    @Schema(description = "设备名称")
    private String name;

    @Comment("说明")
    @Column(name = "describe")
    @Schema(description = "说明")
    private String describe;

    @Comment("产品id")
    @Column(name = "product_id", length = 64)
    @NotBlank(message = "产品ID不能为空", groups = CreateGroup.class)
    @Schema(description = "产品ID")
    private String productId;

    @Comment("产品名称")
    @Column(name = "product_name")
    @NotBlank(message = "产品名称不能为空", groups = CreateGroup.class)
    @Schema(description = "产品名称")
    private String productName;

    @Comment("其他配置")
    @Column(name = "configuration")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "配置信息")
    private Map<String, Object> configuration;

    @Comment("派生元数据,有的设备的属性，功能，事件可能会动态的添加")
    @Column(name = "derive_metadata")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "派生物模型(预留)")
    private String deriveMetadata;

    @Column(name = "state")
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("notActive")
    @Schema(
        description = "状态(只读)"
        //,accessMode = Schema.AccessMode.READ_ONLY
        , defaultValue = "notActive"
    )
    private DeviceState state;

    @Column(name = "creator_id", updatable = false)
    @Schema(
        description = "创建者ID(只读)"
//        ,accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(name = "creator_name", updatable = false)
    @Schema(
        description = "创建者名称(只读)"
        // ,accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorName;

    @Column(name = "create_time", updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        //, accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Comment("激活时间")
    @Column(name = "registry_time")
    @Schema(description = "激活时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long registryTime;

    @Column(name = "org_id", length = 64)
    @Comment("所属机构ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "parent_id", length = 64)
    @Comment("父级设备ID")
    @Schema(description = "父级设备ID")
    private String parentId;

    public DeviceInfo toDeviceInfo() {
        DeviceInfo info = org.jetlinks.core.device.DeviceInfo
            .builder()
            .id(this.getId())
            .productId(this.getProductId())
            .build()
            .addConfig(DeviceConfigKey.parentGatewayId, this.getParentId());
        info.addConfig("deviceName", name);
        info.addConfig("productName", productName);
        if (!CollectionUtils.isEmpty(configuration)) {
            info.addConfigs(configuration);
        }
        if (StringUtils.hasText(deriveMetadata)) {
            info.addConfig(DeviceConfigKey.metadata, deriveMetadata);
        }
        return info;
    }
}
