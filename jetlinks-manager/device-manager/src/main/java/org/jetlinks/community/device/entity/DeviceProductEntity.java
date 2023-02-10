package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.UpdateGroup;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.SimpleDeviceMetadata;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.jetlinks.community.device.enums.DeviceType.gateway;

@Getter
@Setter
@Table(name = "dev_product", indexes = {
    @Index(name = "idx_prod_class_id", columnList = "classified_id")
})
@Comment("产品信息表")
@EnableEntityEvent
public class DeviceProductEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(
        regexp = "^[0-9a-zA-Z_\\-]+$",
        message = "ID只能由数字,字母,下划线和中划线组成",
        groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId() {
        return super.getId();
    }

    @Column(name = "name")
    @NotBlank(message = "产品名称不能为空", groups = CreateGroup.class)
    @Schema(description = "产品名称")
    private String name;

    @Schema(description="所属项目")
    @Column(name = "project_id", length = 64)
    @Hidden
    private String projectId;

    @Column(name = "photo_url", length = 1024)
    @Schema(description = "图片地址")
    private String photoUrl;

    @Schema(description="项目名称")
    @Column(name = "project_name")
    @Hidden
    private String projectName;

    @Column(name = "describe")
    @Schema(description = "说明")
    private String describe;

    @Column(name = "classified_id", length = 64)
    @Schema(description = "所属品类ID")
    private String classifiedId;

    @Column(name = "classified_name")
    @Schema(description = "所属品类名称")
    private String classifiedName;

    @Column(name = "message_protocol")
    @Length(min = 1, max = 256, groups = {
        CreateGroup.class, UpdateGroup.class
    })
    @Schema(description = "消息协议ID")
    private String messageProtocol;

    @Column
    @Schema(description = "消息协议名称")
    @Deprecated
    private String protocolName;

    @Column(name = "metadata")
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "物模型定义")
    private String metadata;

    @Column(name = "transport_protocol")
    @Schema(description = "传输协议")
    private String transportProtocol;

    @Column(name = "network_way")
    @Schema(description = "入网方式")
    private String networkWay;

    @Column(name = "device_type")
    @ColumnType(javaType = String.class)
    @EnumCodec
    @Schema(description = "设备类型")
    private DeviceType deviceType;

    @Column(name = "configuration")
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "协议相关配置")
    private Map<String, Object> configuration;

    @Column(name = "state")
    @DefaultValue("0")
    @Schema(description = "产品状态 1正常,0禁用")
    private Byte state;

    @Column(name = "creator_id", updatable = false)
    @Schema(description = "创建者ID(只读)")
    private String creatorId;

    @Column(name = "create_time", updatable = false)
    @Schema(description = "创建者时间(只读)")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Column(name = "org_id", length = 64)
    @Schema(description = "机构ID")
    @Deprecated
    @Hidden
    private String orgId;

    @Column(length = 64)
    @Schema(description = "设备接入方式ID")
    private String accessId;

    /**
     * @see DeviceGatewayProvider#getId()
     */
    @Column(length = 64)
    @Schema(description = "设备接入方式")
    private String accessProvider;

    @Column
    @Schema(description = "设备接入方式名称")
    private String accessName;

    @Column
    @Schema(description = "数据存储策略")
    private String storePolicy;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "数据存储策略相关配置")
    private Map<String, Object> storePolicyConfiguration;

    @Column(length = 64)
    @Schema(description = "修改人")
    private String modifierId;

    @Column
    @Schema(description = "修改时间")
    private Long modifyTime;

    public Optional<Transport> getTransportEnum(Collection<? extends Transport> candidates) {
        for (Transport transport : candidates) {
            if (transport.isSame(transportProtocol)) {
                return Optional.of(transport);
            }
        }
        return Optional.empty();
    }

    public ProductInfo toProductInfo() {
        return ProductInfo
            .builder()
            .id(getId())
            .protocol(getMessageProtocol())
            .metadata(getMetadata())
            .build()
            .addConfig(DeviceConfigKey.isGatewayDevice, getDeviceType() == gateway)
            .addConfig("storePolicy", storePolicy)
            .addConfig("storePolicyConfiguration", storePolicyConfiguration)
            .addConfig("deviceType", deviceType == null ? "device" : deviceType.getValue())
            .addConfig(PropertyConstants.accessId, accessId)
            .addConfig(PropertyConstants.accessProvider, accessProvider)
            .addConfigs(configuration);
    }

    public DeviceMetadata parseMetadata(){
        if(StringUtils.hasText(metadata)){
            return JetLinksDeviceMetadataCodec.getInstance().doDecode(metadata);
        }
        return new SimpleDeviceMetadata();
    }

    public void validateId() {
        tryValidate(DeviceProductEntity::getId, CreateGroup.class);
    }
}
