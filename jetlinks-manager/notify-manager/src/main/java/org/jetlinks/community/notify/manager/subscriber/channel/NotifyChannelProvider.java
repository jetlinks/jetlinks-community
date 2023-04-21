package org.jetlinks.community.notify.manager.subscriber.channel;

import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface NotifyChannelProvider extends Ordered {

    String getId();

    String getName();

    Mono<NotifyChannel> createChannel(Map<String, Object> configuration);

}
