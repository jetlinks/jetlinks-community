package org.jetlinks.community.notify.manager.subscriber;

import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.core.metadata.ConfigMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface SubscriberProvider {
    String getId();

    String getName();

    Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config);

    ConfigMetadata getConfigMetadata();
}
