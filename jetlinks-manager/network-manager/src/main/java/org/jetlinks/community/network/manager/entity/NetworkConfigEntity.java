package org.jetlinks.community.network.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkProperties;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "network_config")
public class NetworkConfigEntity extends GenericEntity<String> {

    @Column
    @NotNull(message = "名称不能为空")
    private String name;

    @Column
    private String description;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @NotNull(message = "类型不能为空")
    private DefaultNetworkType type;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("disabled")
    private NetworkConfigState state;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    private Map<String, Object> configuration;

    public NetworkProperties toNetworkProperties() {
        NetworkProperties properties = new NetworkProperties();
        properties.setConfigurations(configuration);
        properties.setEnabled(state == NetworkConfigState.enabled);
        properties.setId(getId());
        properties.setName(name);

        return properties;
    }

}
