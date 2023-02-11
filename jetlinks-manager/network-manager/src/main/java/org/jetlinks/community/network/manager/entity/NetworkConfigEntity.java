package org.jetlinks.community.network.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.community.network.NetworkProperties;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.JDBCType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@Table(name = "network_config")
@Comment("网络组件信息表")
@EnableEntityEvent
public class NetworkConfigEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column
    @NotNull(message = "名称不能为空")
    @Schema(description = "名称")
    @Generated
    private String name;

    @Column
    @Generated
    @Schema(description = "说明")
    private String description;

    /**
     * 组件类型
     *
     * @see NetworkType
     * @see org.jetlinks.community.network.NetworkTypes
     */
    @Schema(description="组件类型")
    @Generated
    @Column(nullable = false)
    @NotNull(message = "类型不能为空")
    private String type;

    @Column(nullable = false)
    @EnumCodec
    @Generated
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "状态")
    private NetworkConfigState state;

    @Column(updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    @Generated
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    @Generated
    private Long createTime;

    @Column
    @Generated
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "配置(根据类型不同而不同)")
    private Map<String, Object> configuration;

    @Column
    @Generated
    @DefaultValue("true")
    @Schema(description = "集群是否共享配置")
    private Boolean shareCluster;

    @Column
    @Generated
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "集群配置")
    private List<Configuration> cluster;

    public Optional<Map<String, Object>> getConfig(String serverId) {
        if ((Boolean.FALSE.equals(shareCluster))
            && CollectionUtils.isNotEmpty(cluster)) {
            return cluster.stream()
                          .filter(conf -> serverId.equals(conf.serverId))
                          .findAny()
                          .map(Configuration::getConfiguration);
        }
        return Optional.ofNullable(configuration);
    }

    public NetworkType lookupNetworkType() {
        return NetworkType.lookup(type).orElseGet(() -> NetworkType.of(type));
    }

    public NetworkType getTypeObject() {
        return lookupNetworkType();
    }

    public List<NetworkProperties> toNetworkPropertiesList() {
        if (Boolean.FALSE.equals(shareCluster) && cluster != null) {
            return cluster
                .stream()
                .map(conf -> toNetworkProperties(conf.configuration))
                .collect(Collectors.toList());
        } else {
            return Collections.singletonList(toNetworkProperties(configuration));
        }
    }

    @Getter
    @Setter
    @Generated
    public static class Configuration implements Serializable {
        private String serverId;

        private Map<String, Object> configuration;
    }

    public NetworkProperties toNetworkProperties(Map<String, Object> conf) {
        NetworkProperties properties = new NetworkProperties();
        properties.setConfigurations(conf);
        properties.setEnabled(state == NetworkConfigState.enabled);
        properties.setId(getId());
        properties.setType(getType());
        properties.setName(name);
        return properties;
    }

    public NetworkProperties toNetworkProperties() {

        return toNetworkProperties(configuration);
    }

}
