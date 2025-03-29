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
