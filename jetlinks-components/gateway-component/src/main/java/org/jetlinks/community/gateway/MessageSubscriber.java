package org.jetlinks.community.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 消息订阅器,订阅器用于订阅来自其他连接器的消息.
 *
 * @author zhouhao
 * @see MessageConnection
 * @see MessagePublisher
 * @since 1.0
 */
public interface MessageSubscriber {

    /**
     * 推送消息到订阅器,处理消息应该是无阻塞的,并且快速失败.
     *
     * @param message 消息
     * @return 处理结果
     */
    @Nonnull
    Mono<Void> publish(@Nonnull TopicMessage message);

    /**
     * 监听订阅请求,网关收到订阅请求后才会将对应的广播消息推送{@link this#publish(TopicMessage)}到该订阅器.
     *
     * @return 订阅流
     */
    @Nonnull
    Flux<Subscription> onSubscribe();

    /**
     * 监听取消订阅,取消订阅后,将不会再收到该话题的消息
     *
     * @return 取消订阅流
     */
    @Nonnull
    Flux<Subscription> onUnSubscribe();

    /**
     * 是否共享集群中的消息
     * <p>
     * 如果为<code>true</code>,
     * 则当集群当其他节点收到消息时,也会调用{@link this#publish(TopicMessage)}.
     * ⚠️: 如果同一个订阅者在集群中多个节点进行相同的订阅,则会收到相同的消息. 在一些场景下(比如业务系统消息队列)不建议设置为true.
     * <p>
     * 如果为<code>false</code>
     * 则只会收到当前服务器的消息
     *
     * @return 是否共享集群中的消息
     */
    boolean isShareCluster();
}
