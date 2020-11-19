package org.jetlinks.community.notify.manager.subscriber.providers;

import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.notify.manager.subscriber.Notify;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class DeviceAlarmProvider implements SubscriberProvider {

    private final EventBus eventBus;

    public DeviceAlarmProvider(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getId() {
        return "device_alarm";
    }

    @Override
    public String getName() {
        return "设备告警";
    }

    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("productId", "产品ID", "产品ID,支持通配符:*", StringType.GLOBAL)
            .add("deviceId", "设备ID", "设备ID,支持通配符:*", StringType.GLOBAL)
            .add("productId", "告警ID", "告警ID,支持通配符:*", StringType.GLOBAL)
            ;
    }

    @Override
    public Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config) {
        ValueObject configs = ValueObject.of(config);

        String productId = configs.getString("productId").orElse("*");
        String deviceId = configs.getString("deviceId").orElse("*");
        String alarmId = configs.getString("alarmId").orElse("*");

        Flux<Notify> flux = eventBus
            .subscribe(Subscription.of("device-alarm:" + id,
                String.format("/rule-engine/device/alarm/%s/%s/%s", productId, deviceId, alarmId),
                Subscription.Feature.local
            ))
            .map(msg -> {
                JSONObject json = msg.bodyToJson(true);

                return Notify.of(
                    String.format("设备[%s]发生告警:[%s]!", json.getString("deviceName"), json.getString("alarmName")),
                    json.getString("alarmId"),
                    System.currentTimeMillis()
                );

            });

        return Mono.just(() -> flux);
    }
}
