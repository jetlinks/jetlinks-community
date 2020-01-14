package org.jetlinks.community.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageGatewayManager {

    Mono<MessageGateway> getGateway(String id);

    Flux<MessageGateway> getAllGateway();

}
