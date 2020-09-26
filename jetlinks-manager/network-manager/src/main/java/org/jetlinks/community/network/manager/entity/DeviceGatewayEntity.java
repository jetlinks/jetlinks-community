package org.jetlinks.community.network.manager.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Getter
@Setter
@Table(name = "device_gateway")
public class DeviceGatewayEntity extends GenericEntity<String> {

    @Comment("名称")
    @Column
    @Schema(description = "名称")
    private String name;

    @Comment("类型")
    @Column
    @Schema(description = "网关类型")
    private String provider;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "状态")
    private NetworkConfigState state;

    @Comment("网络组件id")
    @Column(name = "network_id", length = 64)
    @Hidden
    private String networkId;

//    @Comment("支持的协议") 根据provider选择 协议  根据协议选择网络组件
//    @Column
//    private String protocol;

    @Comment("其他配置")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "配置信息(根据类型不同而不同)")
    private Map<String,Object> configuration;

    @Comment("描述")
    @Column
    @Schema(description = "说明")
    private String describe;



}
