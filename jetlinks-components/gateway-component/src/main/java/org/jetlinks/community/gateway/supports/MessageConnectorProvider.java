package org.jetlinks.community.gateway.supports;

import org.jetlinks.community.gateway.MessageConnector;
import reactor.core.publisher.Mono;

public interface MessageConnectorProvider {

    String getId();

    String getName();

    Mono<MessageConnector> createMessageConnector(MessageConnectorProperties properties);

}
