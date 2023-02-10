package org.jetlinks.community.network.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.manager.enums.DeviceGatewayState;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.Map;

/**
 * @author wangzheng
 * @since 1.0
 */
@Getter
@Setter
@Generated
@Table(name = "device_gateway")
@Comment("设备接入网关")
@EnableEntityEvent
public class DeviceGatewayEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column
    @Schema(description = "名称")
    private String name;

    /**
     * @see DeviceGatewayProvider#getId()
     */
    @Column(length = 64)
    @Schema(description = "接入方式,如: mqtt-server-gateway")
    @NotBlank(groups = CreateGroup.class)
    private String provider;

    @Column(length = 32)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "状态")
    @DefaultValue("enabled")
    private DeviceGatewayState state;

    /**
     * @see DeviceGatewayProvider#getChannel()
     */
    @Schema(description = "接入通道(方式),如网络组件")
    @Column(length = 64)
    @NotBlank(groups = CreateGroup.class)
    private String channel;

    //非必填
    @Schema(description = "接入使用的通道ID,如: 网络组件ID,modbus通道ID")
    @Column(length = 64)
    private String channelId;

    /**
     * @see ProtocolSupport#getId()
     */
    @Schema(description = "消息协议")
    @Column(length = 64)
    @NotBlank(groups = CreateGroup.class)
    private String protocol;

    /**
     * @see ProtocolSupport#getSupportedTransport()
     */
    @Schema(description = "传输协议,如TCP,MQTT,UDP")
    @Column(length = 64)
    @NotBlank(groups = CreateGroup.class)
    private String transport;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "配置信息(根据类型不同而不同)")
    private Map<String, Object> configuration;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column(updatable = false, length = 64)
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

    @Column(length = 64)
    @Schema(
        description = "修改人ID"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String modifierId;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "修改时间"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long modifyTime;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "状态变更时间")
    private Long stateTime;


    public DeviceGatewayProperties toProperties(){

        return FastBeanCopier.copy(this, new DeviceGatewayProperties());
    }
}
