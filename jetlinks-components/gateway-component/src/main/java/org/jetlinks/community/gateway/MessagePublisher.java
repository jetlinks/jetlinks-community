package org.jetlinks.community.gateway;

import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;

/**
 * 消息推送器,可通过{@link this#onMessage()}来订阅推送器中到消息
 *
 * @see MessageConnection
 * @since 1.0
 */
public interface MessagePublisher {

    /**
     * 订阅连接中的消息
     *
     * @return 消息流
     */
    @Nonnull
    Flux<TopicMessage> onMessage();


}
