package org.jetlinks.community.notify.manager.subscriber.providers;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.core.event.EventBus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class AlarmSceneProvider extends AlarmProvider {

    public AlarmSceneProvider(EventBus eventBus) {
        super(eventBus);
    }

    @Override
    public String getId() {
        return "alarm-other";
    }

    @Override
    public String getName() {
        return LocaleUtils
            .resolveMessage("message.subscriber.provider.alarm-other", "场景告警");
    }

    @Override
    public Integer getOrder() {
        return 300;
    }

    @Override
    public Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config) {
        String topic = Topics.alarm(TargetType.scene.name(), "*", getAlarmId(config));
        return doCreateSubscriber(id, authentication, topic);
    }

}
