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
                Subscription.Feature.local, Subscription.Feature.broker
            ))
            .map(msg -> Message.success(request.getId(), msg.getTopic(), msg.bodyToJson(true)));
    }
}
