package org.jetlinks.community.notify.manager.subscriber;

import org.jetlinks.core.metadata.ConfigMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface SubscriberProvider {
    String getId();

    String getName();

    Mono<Subscriber> createSubscriber(Map<String, Object> config);

    ConfigMetadata getConfigMetadata();
}
