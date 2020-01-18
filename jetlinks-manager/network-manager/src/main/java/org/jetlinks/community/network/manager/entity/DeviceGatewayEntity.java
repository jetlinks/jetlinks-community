package org.jetlinks.community.network.manager.entity;

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
    private String name;

    @Comment("类型")
    @Column
    private String provider;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("disabled")
    private NetworkConfigState state;

    @Comment("网络组件id")
    @Column(name = "network_id", length = 32)
    private String networkId;

    @Comment("其他配置")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    private Map<String,Object> configuration;

    @Comment("描述")
    @Column
    private String describe;



}
