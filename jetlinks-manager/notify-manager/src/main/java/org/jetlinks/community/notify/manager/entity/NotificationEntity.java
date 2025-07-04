/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.notify.manager.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.jetlinks.community.utils.ObjectMappers;

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
@Comment("消息通知信息表")
public class NotificationEntity extends GenericEntity<String> {
    private static final long serialVersionUID = -1L;

    @Schema(description = "订阅者ID")
    @Column(length = 64, nullable = false, updatable = false)
    @Hidden
    private String subscribeId;

    @Schema(description = "订阅类型")
    @Column(length = 32, nullable = false, updatable = false)
    @Hidden
    private String subscriberType;

    @Schema(description = "订阅者")
    @Column(length = 64, nullable = false, updatable = false)
    @Hidden
    private String subscriber;

    @Column(length = 32, nullable = false, updatable = false)
    @Schema(description = "主题标识,如:device_alarm")
    private String topicProvider;

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "主题名称")
    private String topicName;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "通知消息")
    private String message;

    @Column(length = 64)
    @Schema(description = "数据ID")
    private String dataId;

    @Column(nullable = false)
    @Schema(description = "通知时间")
    private Long notifyTime;

    @Column(length = 128)
    @Schema(description = "通知编码")
    private String code;

    @Column
    @Schema(description = "详情")
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    private String detailJson;

    @Column(length = 32)
    @EnumCodec
    @DefaultValue("unread")
    @ColumnType(javaType = String.class)
    @Schema(description = "通知状态")
    private NotificationState state;

    @Column(length = 1024)
    @Schema(description = "说明")
    private String description;

    @SneakyThrows
    public static NotificationEntity from(Notification notification) {
        NotificationEntity entity = FastBeanCopier.copy(notification, new NotificationEntity());
        Object detail = notification.getDetail();

        entity.setCode(notification.getCode());
        if (detail != null) {
            entity.setDetailJson(ObjectMappers.JSON_MAPPER.writeValueAsString(detail));
        }
        return entity;
    }
}
