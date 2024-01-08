package org.jetlinks.community.notify.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.authorize.AuthenticationSpec;
import org.jetlinks.community.notify.manager.enums.NotifyChannelState;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.Map;

/**
 * 通知通道(配置).
 * 用于定义哪些权限范围(grant),哪种主题(topicProvider),支持何种方式(channel)进行通知
 * <p>
 * 比如: 管理员角色的用户可以使用邮件通知,但是普通用户只能使用站内信通知.
 *
 * @author zhouhao
 * @since 2.0
 */
@Getter
@Setter
@Table(name = "notify_subscriber_channel")
@Schema(description = "通知订阅通道")
@EnableEntityEvent
public class NotifySubscriberChannelEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "主题提供商标识")
    private String providerId;

    @Column(nullable = false, length = 32)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "名称")
    private String name;

    @Column(nullable = false, length = 32)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "通知类型")
    private String channelProvider;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @Schema(description = "权限范围")
    private AuthenticationSpec grant;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @Schema(description = "通知配置")
    private Map<String, Object> channelConfiguration;

    @Column(length = 32)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "状态")
    private NotifyChannelState state;

    @Column(length = 64, updatable = false)
    @Schema(description = "创建人ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorId;

    @Column(length = 64, updatable = false)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;
}
