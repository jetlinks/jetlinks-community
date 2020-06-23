package org.jetlinks.community.network.tcp.executor;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.Assert;

@Getter
@Setter
public class TcpClientTaskConfiguration {

    private String clientId;

    private PubSubType type;

    private PayloadType payloadType;

    public void validate() {
        Assert.hasText(clientId, "clientId can not be empty!");
        Assert.notNull(type, "type can not be null!");
        Assert.notNull(payloadType, "type can not be null!");

    }
}
