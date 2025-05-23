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
package org.jetlinks.community.notify.manager.message;

import lombok.AllArgsConstructor;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@AllArgsConstructor
public class NotificationsPublishProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    @Override
    public String id() {
        return "notifications-publisher";
    }

    @Override
    public String name() {
        return "通知推送器";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{"/notifications"};
    }

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {

        return eventBus
            .subscribe(Subscription.of(
                "notifications-publisher",
                "/notifications/user/" + request.getAuthentication().getUser().getId() + "/*/*",
                Subscription.Feature.local, Subscription.Feature.broker, Subscription.Feature.safetySerialization
            ))
            .map(msg -> Message.success(request.getId(), msg.getTopic(), msg.bodyToJson(true)));
    }
}
