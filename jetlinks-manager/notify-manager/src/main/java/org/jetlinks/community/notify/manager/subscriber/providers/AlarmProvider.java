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
package org.jetlinks.community.notify.manager.subscriber.providers;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.notify.enums.SubscriberTypeEnum;
import org.jetlinks.community.notify.manager.subscriber.Notify;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.subscription.SubscribeType;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.utils.FluxUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
        return LocaleUtils
            .resolveMessage("message.subscriber.provider.alarm", "告警");
    }

    @Override
    public SubscribeType getType() {
        return SubscriberTypeEnum.alarm;
    }

    @Override
    public Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config) {
        String topic = Topics.alarm("*", "*", getAlarmId(config));
        return doCreateSubscriber(id, authentication, topic);
    }

    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("alarmConfigId", "告警规则", "告警规则,支持通配符:*", StringType.GLOBAL);
    }

    protected String getAlarmId(Map<String, Object> config) {
        ValueObject configs = ValueObject.of(config);
        return configs.getString("alarmConfigId").orElse("*");
    }

    protected Mono<Subscriber> doCreateSubscriber(String id,
                                                  Authentication authentication,
                                                  String topic) {
        return Mono.just(locale -> createSubscribe(locale, id, new String[]{topic})
            //有效期内去重,防止同一个用户所在多个部门推送同一个告警
            .as(FluxUtils.distinct(Notify::getDataId, Duration.ofSeconds(10))));
    }

    private Flux<Notify> createSubscribe(Locale locale,
                                         String id,
                                         String[] topic) {
        return this
            .eventBus
            .subscribe(
                Subscription
                    .builder()
                    .justLocal()
                    .subscriberId("alarm:" + id)
                    .topics(topic)
                    .build())
            .mapNotNull(payload -> {
                try {
                    JSONObject json = payload.bodyToJson();
                    return Notify.of(
                        getNotifyMessage(locale, json),
                        //告警记录ID
                        json.getString("id"),
                        System.currentTimeMillis(),
                        "alarm",
                        json
                    );
                } catch (Throwable error) {
                    log.warn("handle alarm notify error", error);
                }
                return null;
            });
    }

    private static String getNotifyMessage(Locale locale, JSONObject json) {

        String message;
        TargetType targetType = TargetType.of(json.getString("targetType"));
        String targetName = json.getString("targetName");
        String alarmName = json.getString("alarmConfigName");
        message = String.format("%s[%s]发生告警:[%s]!", targetType.getText(), targetName, alarmName);
        return LocaleUtils.resolveMessage("message.alarm.notify." + targetType.name(), locale, message, targetName, alarmName);
    }

    @Override
    public Flux<PropertyMetadata> getDetailProperties(Map<String, Object> config) {
        //todo 根据配置来获取输出数据
        return Flux.just(
            SimplePropertyMetadata.of("targetType", "告警类型", StringType.GLOBAL),
            SimplePropertyMetadata.of("alarmConfigName", "告警名称", StringType.GLOBAL),
            SimplePropertyMetadata.of("targetName", "告警目标名称", StringType.GLOBAL),
            SimplePropertyMetadata.of("level", "告警级别", IntType.GLOBAL),
            SimplePropertyMetadata.of("alarmTime", "告警时间", LongType.GLOBAL),
            SimplePropertyMetadata.of("sourceType", "告警源类型", StringType.GLOBAL),
            SimplePropertyMetadata.of("sourceName", "告警源名称", StringType.GLOBAL)
        );
    }

    @AllArgsConstructor
    @Getter
    enum TargetType {
        device("设备"),
        product("产品"),
        scene("场景");
        private final String text;

        public static TargetType of(String name) {
            for (TargetType value : TargetType.values()) {
                if (Objects.equals(value.name(), name)) {
                    return value;
                }
            }
            return TargetType.scene;
        }
    }
}
