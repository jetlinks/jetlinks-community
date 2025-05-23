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
package org.jetlinks.community.notify;

import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.notify.template.Template;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 通知服务提供商
 *
 * @author zhouhao
 * @see TemplateProvider
 * @see NotifierManager
 * @since 1.0
 */
public interface NotifierProvider {

    /**
     * 获取通知类型
     *
     * @return 通知类型
     * @see DefaultNotifyType
     */
    @Nonnull
    NotifyType getType();

    /**
     * @return 服务商
     */
    @Nonnull
    Provider getProvider();

    /**
     * 根据配置创建通知器
     *
     * @param properties 通知配置
     * @return 创建结果
     */
    @Nonnull
    Mono<? extends Notifier<? extends Template>> createNotifier(@Nonnull NotifierProperties properties);

    /**
     * 获取通知配置元数据,通过元数据可以知道此通知所需要的配置信息
     *
     * @return 配置元数据
     */
    @Nullable
    default ConfigMetadata getNotifierConfigMetadata() {
        return null;
    }
}
