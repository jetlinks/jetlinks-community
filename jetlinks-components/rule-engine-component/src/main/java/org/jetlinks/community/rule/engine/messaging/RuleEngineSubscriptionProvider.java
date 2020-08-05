package org.jetlinks.community.rule.engine.messaging;

import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@Component
public class RuleEngineSubscriptionProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    public RuleEngineSubscriptionProvider(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String id() {
        return "rule-engine";
    }

    @Override
    public String name() {
        return "规则引擎";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{"/rule-engine/**"};
    }

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {
        String subscriber=request.getId();

        org.jetlinks.core.event.Subscription subscription = org.jetlinks.core.event.Subscription.of(subscriber,request.getTopic(), org.jetlinks.core.event.Subscription.Feature.local, Subscription.Feature.broker);

        return eventBus
            .subscribe(subscription)
            .map(msg -> Message.success(request.getId(), msg.getTopic(), msg.decode()));
    }
}
