package org.jetlinks.community.gateway.rule;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

@Getter
@Setter
public class MessageGatewayRuleNodeConfig implements RuleNodeConfig {

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

    @Override
    public void validate() {
        Assert.hasText(gatewayId, "gatewayId can not be empty");
        Assert.hasText(topics, "topics can not be empty");
        Assert.notNull(type, "type can not be null");

    }

    @Override
    public NodeType getNodeType() {
        return NodeType.MAP;
    }


    @Override
    public void setNodeType(NodeType nodeType) {

    }
}