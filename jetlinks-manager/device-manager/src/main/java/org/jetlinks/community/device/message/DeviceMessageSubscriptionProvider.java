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
package org.jetlinks.community.device.message;

import lombok.Generated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "jetlinks.messaging.device-message-subscriber")
public class DeviceMessageSubscriptionProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    @Getter
    @Setter
    //是否使用真实产生的topic作为响应
    //当订阅者进行了数据权限控制等场景时实际发生的数据topic可能与订阅的topic不一致
    private boolean responseActualTopic = true;

    @Override
    @Generated
    public String id() {
        return "device-message-subscriber";
    }

    @Override
    @Generated
    public String name() {
        return "订阅设备消息";
    }

    @Override
    @Generated
    public String[] getTopicPattern() {
        return new String[]{
            //直接订阅设备
            "/device/*/*/**",
            //按维度订阅
            "/*/*/device/*/*/**"
        };
    }

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {
        return eventBus
            .subscribe(
                org.jetlinks.core.event.Subscription.of(
                    "DeviceMessageSubscriptionProvider:" + request.getAuthentication().getUser().getId(),
                    new String[]{request.getTopic()},
                    org.jetlinks.core.event.Subscription.Feature.local,
                    Subscription.Feature.broker
                ))
            .map(topicMessage -> Message.success(request.getId(), topicMessage.getTopic(), topicMessage.decode()));
    }
}
