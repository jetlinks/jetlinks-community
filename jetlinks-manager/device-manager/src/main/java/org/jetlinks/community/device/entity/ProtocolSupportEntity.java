package org.jetlinks.community.device.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_protocol")
public class ProtocolSupportEntity extends GenericEntity<String> {

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    private Byte state;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    private Map<String, Object> configuration;

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
        definition.setProvider(type);
        definition.setState((byte) 1);

        return definition;
    }
}
