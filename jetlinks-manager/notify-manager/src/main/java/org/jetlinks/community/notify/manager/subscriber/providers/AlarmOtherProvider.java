package org.jetlinks.community.notify.manager.subscriber.providers;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.topic.Topics;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class AlarmOtherProvider extends AlarmProvider {

    public AlarmOtherProvider(EventBus eventBus) {
        super(eventBus);
    }

    @Override
    public String getId() {
        return "alarm-other";
    }

    @Override
    public String getName() {
        return "其他告警";
    }

    @Override
    public Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config) {
        String topic = Topics.alarm(TargetType.other.name(), "*", getAlarmId(config));
        return doCreateSubscriber(id, authentication, topic);
    }

}
