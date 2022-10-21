package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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
import org.hswebframework.web.utils.DigestUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.things.ThingMetadataType;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_metadata_mapping", indexes = {
    @Index(name = "idx_dev_mmp_did", columnList = "device_id"),
    @Index(name = "idx_dev_mmp_pid", columnList = "product_id")
})
@Schema(description = "设备物模型映射")
@EnableEntityEvent
public class DeviceMetadataMappingEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Schema(description = "产品ID")
    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String productId;

    @Schema(description = "设备ID,为空时表示映射对产品下所有设备生效")
    @Column(length = 64, updatable = false)
    @NotBlank
    private String deviceId;

    @Schema(description = "物模型类型,如:property")
    @Column(length = 32, nullable = false, updatable = false)
    @DefaultValue("property")
    @NotNull(groups = CreateGroup.class)
    @EnumCodec
    @ColumnType(javaType = String.class)
    private ThingMetadataType metadataType;

    @Schema(description = "物模型ID,如:属性ID")
    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String metadataId;

    @Schema(description = "原始物模型ID")
    @Column(length = 64, nullable = false)
    @NotBlank(groups = CreateGroup.class)
    private String originalId;

    @Schema(description = "其他配置")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    private Map<String, Object> others;

    @Schema(description = "说明")
    @Column(length = 512)
    private String description;

    @Schema(description = "创建者ID", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(length = 64, updatable = false)
    private String creatorId;

    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Override
    public String getId() {
        if (super.getId() == null) {
            generateId();
        }
        return super.getId();
    }

    public void generateId() {
        if (StringUtils.hasText(deviceId)) {
            setId(
                generateIdByDevice(deviceId, metadataType, metadataId)
            );
        } else if (StringUtils.hasText(productId)) {
            setId(
                generateIdByProduct(productId, metadataType, metadataId)
            );
        }
    }

    public static String generateIdByProduct(String productId, ThingMetadataType type, String metadataId) {
        return DigestUtils.md5Hex(String.join(":", "product", productId, type.name(), metadataId));
    }

    public static String generateIdByDevice(String deviceId, ThingMetadataType type, String metadataId) {
        return DigestUtils.md5Hex(String.join(":", "device", deviceId, type.name(), metadataId));
    }
}
