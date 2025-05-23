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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.authorize.AuthenticationSpec;
import org.jetlinks.community.notify.manager.enums.NotifyChannelState;

import javax.persistence.Column;
import javax.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.Map;

/**
 * 通知订阅提供商,用于定义用户支持订阅何种通知.
 *
 * @author zhouhao
 * @see org.jetlinks.pro.notify.subscription.SubscriberProvider
 * @since 2.0
 */
@Getter
@Setter
@Table(name = "notify_subscriber_provider")
@Schema(description = "通知订阅提供商")
@EnableEntityEvent
public class NotifySubscriberProviderEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column(length = 64, nullable = false)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "名称")
    private String name;

    /**
     * @see org.jetlinks.pro.notify.subscription.SubscriberProvider#getId()
     */
    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "订阅提供商ID", accessMode = Schema.AccessMode.READ_ONLY)
    @NotBlank(groups = CreateGroup.class)
    private String provider;

    /**
     * @see org.jetlinks.pro.notify.subscription.SubscriberProvider#createSubscriber(String, Authentication, Map)
     */
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @Schema(description = "配置信息")
    private Map<String, Object> configuration;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @Schema(description = "权限范围")
    private AuthenticationSpec grant;

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
