package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@AllArgsConstructor
public class DeviceMessageSubscriptionProvider implements SubscriptionProvider {

    private final MessageGateway messageGateway;

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
        return messageGateway
            .subscribe(Collections.singletonList(new Subscription(request.getTopic())), true)
            .flatMap(topic -> Mono.justOrEmpty(DeviceMessageUtils.convert(topic)
                .map(msg -> Message.success(request.getId(), topic.getTopic(), msg))
            ));
    }
}
