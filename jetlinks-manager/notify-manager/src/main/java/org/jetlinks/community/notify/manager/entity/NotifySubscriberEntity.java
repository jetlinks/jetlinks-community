package org.jetlinks.community.notify.manager.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.jetlinks.community.notify.manager.enums.SubscribeState;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通知订阅者
 *
 * @author zhouhao
 * @since 1.3
 */
@Table(name = "notify_subscribers",
    indexes = @Index(name = "idx_nfy_subs_subscriber", columnList = "subscriber")
)
@Getter
@Setter
@EnableEntityEvent
public class NotifySubscriberEntity extends GenericEntity<String> {

    private static final long serialVersionUID = -1L;

    @Comment("订阅者类型,如:user")
    @Column(length = 32, nullable = false, updatable = false)
    @Hidden
    private String subscriberType;

    @Comment("订阅者ID")
    @Column(length = 32, nullable = false, updatable = false)
    @Hidden
    private String subscriber;

    @Comment("主题提供商标识,如:device_alarm")
    @Column(length = 32, nullable = false, updatable = false)
    @Schema(description = "主题标识,如:device_alarm")
    private String topicProvider;

    @Comment("订阅名称")
    @Column(length = 64, nullable = false)
    @Schema(description = "订阅名称")
    private String subscribeName;

    @Comment("主题名称,如:设备告警")
    @Column(length = 64, nullable = false)
    @Schema(description = "主题名称")
    private String topicName;

    @Comment("主题订阅配置")
    @Column(length = 3000)
    @JsonCodec
    @ColumnType
    @Schema(description = "订阅配置,根据主题标识不同而不同.")
    private Map<String, Object> topicConfig;

    @Column
    @Comment("描述")
    @Schema(description = "说明")
    private String description;

    @Comment("状态:enabled,disabled")
    @Column(length = 32)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "状态.")
    private SubscribeState state;

    @Column(length = 32)
    @Schema(description = "订阅语言")
    private String locale;


    /**
     * @see NotifyChannelEntity#getId()
     */
    @Column(length = 3000)
    @Schema(description = "通知方式")
    @JsonCodec
    @ColumnType(javaType = String.class)
    private List<String> notifyChannels;


    public Locale toLocale() {
        return locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale);
    }
}
