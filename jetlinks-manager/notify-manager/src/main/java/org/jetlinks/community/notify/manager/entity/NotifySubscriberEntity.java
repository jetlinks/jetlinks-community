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
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.springframework.util.StringUtils;

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

    @Schema(description = "订阅者类型,如:user")
    @Column(length = 32, nullable = false, updatable = false)
    @Hidden
    private String subscriberType;

    @Schema(description = "订阅者ID")
    @Column(length = 32, nullable = false, updatable = false)
    @Hidden
    private String subscriber;

    @Column(length = 32, nullable = false, updatable = false)
    @Schema(description = "主题标识,如:device_alarm")
    private String topicProvider;

    /**
     * @see NotifySubscriberProviderEntity#getId()
     */
    @Column(length = 64)
    @Schema(description = "订阅提供商ID")
    private String providerId;

    @Column(length = 64, nullable = false)
    @Schema(description = "订阅名称")
    private String subscribeName;

    @Column(length = 64, nullable = false)
    @Schema(description = "主题名称")
    private String topicName;

    @Column(length = 3000)
    @JsonCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "订阅配置,根据主题标识不同而不同.")
    private Map<String, Object> topicConfig;

    @Column
    @Comment("描述")
    @Schema(description = "说明")
    private String description;

    @Column(length = 32)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "订阅状态")
    private SubscribeState state;

    @Column(length = 32)
    @Schema(description = "订阅语言")
    private String locale;

    /**
     * @see NotifySubscriberChannelEntity#getId()
     */
    @Column(length = 3000)
    @Schema(description = "通知方式")
    @JsonCodec
    @ColumnType(javaType = String.class)
    private List<String> notifyChannels;

    public String generateId() {
        if (super.getId() == null
            && StringUtils.hasText(subscriberType)
            && StringUtils.hasText(subscriber)
            && StringUtils.hasText(topicProvider)) {
            return DigestUtils.md5Hex(String.join(":", subscriberType, subscriber, topicProvider));
        }
        return null;
    }

    public Locale toLocale() {
        return locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale);
    }

}
