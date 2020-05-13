package org.jetlinks.community.gateway.external;

import reactor.core.publisher.Flux;

public interface SubscriptionProvider {

    String id();

    String name();

    String[] getTopicPattern();

    Flux<?> subscribe(SubscribeRequest request);

}
