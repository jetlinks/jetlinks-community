package org.jetlinks.community.notify.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.notify.manager.enums.NotificationState;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "notify_notifications",
    indexes = @Index(
        name = "idx_ntfc_subscribe", columnList = "subscriber_type,subscriber"
    ))
public class NotificationEntity extends GenericEntity<String> {
    private static final long serialVersionUID = -1L;

    @Column(length = 64, nullable = false, updatable = false)
    private String subscribeId;

    @Column(length = 32, nullable = false, updatable = false)
    private String subscriberType;

    @Column(length = 64, nullable = false, updatable = false)
    private String subscriber;

    @Column(length = 32, nullable = false, updatable = false)
    private String topicProvider;

    @Column(length = 64, nullable = false, updatable = false)
    private String topicName;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    private String message;

    @Column(length = 64)
    private String dataId;

    @Column(nullable = false)
    private Long notifyTime;

    @Column(length = 32)
    @EnumCodec
    @DefaultValue("unread")
    @ColumnType(javaType = String.class)
    private NotificationState state;

    @Column(length = 1024)
    private String description;

    public static NotificationEntity from(Notification notification) {
        return FastBeanCopier.copy(notification, new NotificationEntity());
    }
}
