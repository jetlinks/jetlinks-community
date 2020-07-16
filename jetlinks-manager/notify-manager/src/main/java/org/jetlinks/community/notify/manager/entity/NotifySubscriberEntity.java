package org.jetlinks.community.notify.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.jetlinks.community.notify.manager.enums.SubscribeState;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
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
    private String subscriberType;

    @Comment("订阅者ID")
    @Column(length = 32, nullable = false, updatable = false)
    private String subscriber;

    @Comment("主题提供商标识,如:device_alarm")
    @Column(length = 32, nullable = false, updatable = false)
    private String topicProvider;

    @Comment("订阅名称")
    @Column(length = 64, nullable = false)
    private String subscribeName;

    @Comment("主题名称,如:设备告警")
    @Column(length = 64, nullable = false)
    private String topicName;

    @Comment("主题订阅配置")
    @Column(length = 3000)
    @JsonCodec
    @ColumnType
    private Map<String, Object> topicConfig;

    @Column
    @Comment("描述")
    private String description;

    @Comment("状态:enabled,disabled")
    @Column(length = 32)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    private SubscribeState state;


}
