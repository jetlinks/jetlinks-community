package org.jetlinks.community.rule.engine.messaging;

import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@Component
public class RuleEngineSubscriptionProvider implements SubscriptionProvider {

    private final MessageGateway messageGateway;

    public RuleEngineSubscriptionProvider(MessageGateway messageGateway) {
        this.messageGateway = messageGateway;
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

        return messageGateway.subscribe(Subscription.asList(request.getTopic()),messageGateway.nextSubscriberId( "rule:sub:" + request.getId()), true)
            .map(msg -> Message.success(request.getId(), msg.getTopic(), msg.convertMessage()));
    }
}
