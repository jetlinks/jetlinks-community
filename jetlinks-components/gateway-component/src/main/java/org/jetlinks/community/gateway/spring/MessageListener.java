package org.jetlinks.community.gateway.spring;

import org.jetlinks.community.gateway.TopicMessage;
import reactor.core.publisher.Mono;

public interface MessageListener {

   Mono<Void> onMessage(TopicMessage message);

}
