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
package org.jetlinks.community.notify.manager.subscriber;

import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.community.notify.enums.SubscriberTypeEnum;
import org.jetlinks.community.notify.subscription.SubscribeType;
import org.jetlinks.core.Wrapper;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 个人订阅提供商,用于提供对不同类型的个人订阅的支持. 如: 设备告警等.
 *
 * @author zhouhao
 * @since 2.2
 */
public interface SubscriberProvider extends Wrapper {
    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 名称
     */
    String getName();

    /**
     * return 排序
     */
    default Integer getOrder(){
        return Integer.MAX_VALUE;
    };

    /**
     * 订阅类型
     *
     * @see SubscriberTypeEnum
     */
    default SubscribeType getType(){
        return SubscriberTypeEnum.other;
    }

    /**
     * 基于配置信息创建订阅器
     *
     * @param id             订阅ID
     * @param authentication 用户权限信息
     * @param config         订阅配置信息
     * @return 订阅器
     */
    Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config);

    /**
     * 获取通过该订阅提供商收到的通知中的{@link Notify#getDetail()}的数据结构,
     * 通常用于前端进行可视化配置: 如和消息通知中的变量进行绑定.
     *
     * @param config 配置信息
     * @return PropertyMetadata
     */
    default Flux<PropertyMetadata> getDetailProperties(Map<String, Object> config) {
        return Flux.empty();
    }

    /**
     * 获取需要的配置结构定义,通常用于前端进行动态渲染配置.
     *
     * @return ConfigMetadata
     */
    ConfigMetadata getConfigMetadata();
}
