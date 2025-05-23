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

import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 通知配置管理器,用于统一管理通知配置
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface NotifyConfigManager {

    /**
     * 根据类型和配置ID获取通知器配置
     * <p>
     * 如果配置不存在则返回{@link Mono#empty()},可通过{@link Mono#switchIfEmpty(Mono)}进行处理
     *
     * @param notifyType 类型 {@link DefaultNotifyType}
     * @param configId   配置ID
     * @return 配置
     */
    @Nonnull
    Mono<NotifierProperties> getNotifyConfig(@Nonnull NotifyType notifyType, @Nonnull String configId);

}
