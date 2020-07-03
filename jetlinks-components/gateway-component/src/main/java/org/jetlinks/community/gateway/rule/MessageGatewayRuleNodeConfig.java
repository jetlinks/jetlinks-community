package org.jetlinks.community.gateway.rule;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.rule.engine.api.RuleData;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

@Getter
@Setter
public class MessageGatewayRuleNodeConfig {

    private String gatewayId;

    private PubSubType type;

    private String topics;

    private boolean shareCluster;

    public Flux<TopicMessage> convert(RuleData data) {
        return TopicMessageCodec.getInstance()
            .decode(data, TopicMessageCodec.feature(createTopics()));
    }

    public Object convert(TopicMessage message) {
        return TopicMessageCodec.getInstance().encode(message);
    }

    public String[] createTopics() {
        return topics.split("[,;\n]");
    }

    public void validate() {
        Assert.hasText(gatewayId, "gatewayId can not be empty");
        Assert.hasText(topics, "topics can not be empty");
        Assert.notNull(type, "type can not be null");

    }

}