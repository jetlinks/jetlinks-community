package org.jetlinks.community.notify.manager.subscriber.providers;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.notify.manager.subscriber.Notify;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.utils.FluxUtils;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Slf4j
public class AlarmProvider implements SubscriberProvider {

    private final EventBus eventBus;

    public AlarmProvider(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getId() {
        return "alarm";
    }

    @Override
    public String getName() {
        return "告警";
    }

    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("alarmConfigId", "告警规则", "告警规则,支持通配符:*", StringType.GLOBAL);
    }

    @Override
    public Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config) {
        ValueObject configs = ValueObject.of(config);

        String alarmId = configs.getString("alarmConfigId").orElse("*");

        String topic = Topics.alarm("*", "*", alarmId);

        return Mono.just(locale -> createSubscribe(locale, id, new String[]{topic})
            //有效期内去重,防止同一个用户所在多个部门推送同一个告警
            .as(FluxUtils.distinct(Notify::getDataId, Duration.ofSeconds(10))));

    }

    private Flux<Notify> createSubscribe(Locale locale,
                                         String id,
                                         String[] topics) {
        Subscription.Feature[] features = new Subscription.Feature[]{Subscription.Feature.local};
        return Flux
            .defer(() -> this
                .eventBus
                .subscribe(Subscription.of("alarm:" + id, topics, features))
                .map(msg -> {
                    JSONObject json = msg.bodyToJson();
                    return Notify.of(
                        getNotifyMessage(locale, json),
                        //告警记录ID
                        json.getString("id"),
                        System.currentTimeMillis(),
                        "alarm",
                        json
                    );
                }));
    }


    private static String getNotifyMessage(Locale locale, JSONObject json) {

        String message;
        TargetType targetType = TargetType.of(json.getString("targetType"));
        String targetName = json.getString("targetName");
        String alarmName = json.getString("alarmConfigName");
        if (targetType == TargetType.other) {
            message = String.format("[%s]发生告警:[%s]!", targetName, alarmName);
        } else {
            message = String.format("%s[%s]发生告警:[%s]!", targetType.getText(), targetName, alarmName);
        }
        return LocaleUtils.resolveMessage("message.alarm.notify." + targetType.name(), locale, message, targetName, alarmName);
    }

    @Override
    public Flux<PropertyMetadata> getDetailProperties(Map<String, Object> config) {
        //todo 根据配置来获取输出数据
        return Flux.just(
            SimplePropertyMetadata.of("targetType", "告警类型", StringType.GLOBAL),
            SimplePropertyMetadata.of("alarmName", "告警名称", StringType.GLOBAL),
            SimplePropertyMetadata.of("targetName", "目标名称", StringType.GLOBAL)
        );
    }

    @AllArgsConstructor
    @Getter
    enum TargetType {
        device("设备"),
        product("产品"),
        other("其它");

        private final String text;

        public static TargetType of(String name) {
            for (TargetType value : TargetType.values()) {
                if (Objects.equals(value.name(), name)) {
                    return value;
                }
            }
            return TargetType.other;
        }
    }
}
