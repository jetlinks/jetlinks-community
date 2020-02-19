package org.jetlinks.community.notify.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.jetlinks.community.notify.NotifierProperties;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.HashMap;
import java.util.Map;

@Table(name = "notify_config")
@Getter
@Setter
@EnableEntityEvent
public class NotifyConfigEntity extends GenericEntity<String> {

    /**
     * 配置名称
     */
    @Column
    private String name;

    /**
     * 通知类型
     */
    @Column
    private String type;

    /**
     * 服务提供商
     */
    @Column
    private String provider;

    /**
     * 描述
     */
    @Column
    private String description;

    /**
     * 配置详情
     */
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    private Map<String, Object> configuration;

    public NotifierProperties toProperties() {
        NotifierProperties properties = new NotifierProperties();
        properties.setProvider(provider);
        properties.setId(getId());
        properties.setType(type);
        properties.setConfiguration(configuration == null ? new HashMap<>() : configuration);
        properties.setName(name);
        return properties;
    }
}
