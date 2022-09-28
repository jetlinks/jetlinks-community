package org.jetlinks.community.notify.manager.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.notify.NotifierProperties;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Table(name = "notify_config")
@Comment("消息通知配置表")
@Getter
@Setter
@EnableEntityEvent
public class NotifyConfigEntity extends GenericEntity<String> implements RecordCreationEntity {
    private static final long serialVersionUID = -6849794470754667710L;

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$",  groups = CreateGroup.class)
    @Schema(description = "设备ID(只能由数字,字母,下划线和中划线组成)")
    public String getId() {
        return super.getId();
    }

    /**
     * 配置名称
     */
    @Column
    @Schema(description = "配置名称")
    @NotBlank(groups = CreateGroup.class)
    private String name;

    /**
     * 通知类型
     */
    @Column
    @Schema(description = "通知类型")
    @NotBlank(groups = CreateGroup.class)
    private String type;

    /**
     * 服务提供商
     */
    @Column
    @Schema(description = "服务提供商")
    @NotBlank(groups = CreateGroup.class)
    private String provider;

    /**
     * 描述
     */
    @Column
    @Schema(description = "描述")
    private String description;

    @Column(length = 512)
    @JsonCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "重试策略,如: [\"1s\",\"20s\",\"5m\",\"15m\"]")
    @Hidden//暂未实现
    private List<String> retryPolicy;

    @Column
    @DefaultValue("0")
    @Schema(description = "最大重试次数")
    @Hidden//暂未实现
    private Integer maxRetryTimes;

    @Column(updatable = false)
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

    /**
     * 配置详情
     */
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "配置信息")
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
