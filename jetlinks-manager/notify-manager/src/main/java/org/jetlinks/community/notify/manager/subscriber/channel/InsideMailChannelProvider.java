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
package org.jetlinks.community.notify.manager.subscriber.channel;

import lombok.AllArgsConstructor;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 站内信通知,通过推送通知信息到事件总线.
 * <p>
 * 由{@link org.jetlinks.community.notify.manager.message.NotificationsPublishProvider}推送到前端.
 * <p>
 * 由{@link org.jetlinks.community.notify.manager.service.NotificationService}写入到数据库.
 *
 * @author zhouhao
 * @since 2.0
 */
@Component
@AllArgsConstructor
public class InsideMailChannelProvider implements NotifyChannelProvider, NotifyChannel {
    public static final String provider = "inside-mail";

    private final EventBus eventBus;

    @Override
    public String getId() {
        return "inside-mail";
    }

    @Override
    public String getName() {
        return LocaleUtils
            .resolveMessage("message.subscriber.provider.inside-mail", "站内信");
    }

    @Override
    public Mono<NotifyChannel> createChannel(Map<String, Object> configuration) {
        return Mono.just(this);
    }

    @Override
    public Mono<Void> sendNotify(Notification notification) {
        //设置了站内信的订阅才推送的事件总线
        return eventBus
            .publish(notification.createTopic(), notification)
            .then();
    }

    @Override
    public void dispose() {

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
