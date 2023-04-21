package org.jetlinks.community.notify.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.validator.CreateGroup;
//import org.jetlinks.community.authorize.AuthenticationSpec;
import org.jetlinks.community.notify.manager.enums.NotifyChannelState;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.channel.NotifyChannelProvider;

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
@Table(name = "notify_channel")
@Getter
@Setter
@Schema(description = "通知通道(配置)")
public class NotifyChannelEntity extends GenericEntity<String> {

    @Column(nullable = false, length = 32)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "名称")
    private String name;

//    @Column
//    @JsonCodec
//    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
//    @Schema(description = "权限范围")
//    private AuthenticationSpec grant;

    /**
     * @see SubscriberProvider#getId()
     */
    @Column(nullable = false, length = 32, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "主题提供商标识")
    private String topicProvider;

    @Column(nullable = false, length = 32)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "主题提供商名称")
    private String topicName;

    /**
     * @see NotifyChannelProvider#getId()
     */
    @Column(nullable = false, length = 32, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "通知类型")
    private String channelProvider;

    /**
     * @see NotifyChannelProvider#createChannel(Map)
     * @see org.jetlinks.community.notify.manager.subscriber.channel.notifiers.NotifierChannelProvider.NotifyChannelConfig
     */
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

}
