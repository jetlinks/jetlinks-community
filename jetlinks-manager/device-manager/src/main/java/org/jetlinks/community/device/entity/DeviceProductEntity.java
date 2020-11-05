package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.UpdateGroup;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.device.enums.DeviceType;

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
public class DeviceProductEntity extends GenericEntity<String> implements RecordCreationEntity {

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

    @Comment("名称")
    @Column(name = "name")
    @NotBlank(message = "产品名称不能为空", groups = CreateGroup.class)
    @Schema(description = "产品名称")
    private String name;

    @Comment("所属项目")
    @Column(name = "project_id", length = 64)
    @Hidden
    private String projectId;

    @Comment("图片地址")
    @Column(name = "photo_url", length = 1024)
    @Schema(description = "图片地址")
    private String photoUrl;

    @Comment("项目名称")
    @Column(name = "project_name")
    @Hidden
    private String projectName;

    @Comment("说明")
    @Column(name = "describe")
    @Schema(description = "说明")
    private String describe;

    @Comment("分类ID")
    @Column(name = "classified_id", length = 64)
    @Schema(description = "所属品类ID")
    private String classifiedId;

    @Comment("分类名称")
    @Column(name = "classified_name")
    @Schema(description = "所属品类名称")
    private String classifiedName;

    @Comment("消息协议: Alink,JetLinks")
    @Column(name = "message_protocol")
    @NotBlank(message = "消息协议不能为空", groups = CreateGroup.class)
    @Length(min = 1, max = 256, groups = {
        CreateGroup.class, UpdateGroup.class
    })
    @Schema(description = "消息协议ID")
    private String messageProtocol;

    @Comment("消息协议名称")
    @Column
    @Schema(description = "消息协议名称")
    private String protocolName;

    @Comment("物模型")
    @Column(name = "metadata")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "物模型定义")
    private String metadata;

    @Comment("传输协议: MQTT,COAP,UDP")
    @Column(name = "transport_protocol")
    @Schema(description = "传输协议")
    private String transportProtocol;

    @Comment("入网方式: 直连,组网...")
    @Column(name = "network_way")
    @Schema(description = "入网方式")
    private String networkWay;

    @Comment("设备类型: 网关，设备")
    @Column(name = "device_type")
    @ColumnType(javaType = String.class)
    @EnumCodec
    @Schema(description = "设备类型")
    private DeviceType deviceType;

    @Comment("协议配置")
    @Column(name = "configuration")
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "协议相关配置")
    private Map<String, Object> configuration;

    @Comment("产品状态")
    @Column(name = "state")
    @DefaultValue("0")
    @Schema(description = "产品状态 1已发布,0未发布")
    private Byte state;

    @Column(name = "creator_id")
    @Comment("创建者id")
    @Schema(description = "创建者ID(只读)")
    private String creatorId;

    @Comment("创建时间")
    @Column(name = "create_time")
    @Schema(description = "创建者时间(只读)")
    private Long createTime;

    @Column(name = "org_id", length = 64)
    @Comment("所属机构id")
    @Schema(description = "机构ID")
    private String orgId;

    @Column
    @Comment("数据存储策略")
    @Schema(description = "数据存储策略")
    private String storePolicy;

    @Comment("数据存储策略配置")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "数据存储策略相关配置")
    private Map<String, Object> storePolicyConfiguration;

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
            .addConfigs(configuration);
    }

}
