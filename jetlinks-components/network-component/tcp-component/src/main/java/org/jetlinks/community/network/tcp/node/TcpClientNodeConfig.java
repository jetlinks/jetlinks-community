package org.jetlinks.community.network.tcp.node;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.springframework.util.Assert;

@Getter
@Setter
public class TcpClientNodeConfig implements RuleNodeConfig {

    private String clientId;

    private PubSubType type;

    private PayloadType sendPayloadType;

    private PayloadType subPayloadType;

    @Override
    public NodeType getNodeType() {
        return NodeType.MAP;
    }

    @Override
    public void setNodeType(NodeType nodeType) {

    }

    @Override
    public void validate() {
        Assert.hasText(clientId, "clientId can not be empty!");
        Assert.notNull(type, "type can not be null!");

    }
}
