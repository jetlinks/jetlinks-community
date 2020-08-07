package org.jetlinks.community.device.message;

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
public class DeviceMessageSubscriptionProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    @Override
    public String id() {
        return "device-message-subscriber";
    }

    @Override
    public String name() {
        return "订阅设备消息";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/device/*/*/**"
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
