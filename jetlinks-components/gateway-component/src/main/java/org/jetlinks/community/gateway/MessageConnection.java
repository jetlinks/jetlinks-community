package org.jetlinks.community.gateway;

import reactor.core.publisher.Mono;

/**
 * 消息连接,在网络组件中,一个客户端则可能认为是一个消息连接.
 * <p>
 * 如果实现了{@link MessagePublisher}接口,则认为可以从此连接中订阅消息,进行网关转发.
 * <p>
 * 如果实现了{@link MessageSubscriber}接口,则认为连接可以进行消息订阅,并接收来自网关的消息.
 *
 * @see MessagePublisher
 * @see MessageSubscriber
 */
public interface MessageConnection {

    /**
     * @return 连接唯一标识
     */
    String getId();

    /**
     * 添加断开连接监听器,当网络断开时,执行{@link Runnable#run()},支持多个监听器
     *
     * @param disconnectListener 监听器
     */
    void onDisconnect(Runnable disconnectListener);

    /**
     * 主动断开连接
     */
    void disconnect();

    /**
     * 当前连接是否存活
     *
     * @return 是否存活
     */
    boolean isAlive();

    /**
     * 判断是否为推送器,如果是则可以使用{@link this#asPublisher()}转为推送器,然后进行消息订阅等处理.
     *
     * @return 是否为推送器
     * @see MessagePublisher
     * @see this#asPublisher()
     */
    default boolean isPublisher() {
        return this instanceof MessagePublisher;
    }

    /**
     * 同{@link this#isPublisher()}
     *
     * @return 是否为订阅器
     */
    default boolean isSubscriber() {
        return this instanceof MessageSubscriber;
    }

    /**
     * 尝试转为消息推送器,如果不可行则返回{@link Mono#empty()}
     *
     * @return MessagePublisher
     * @see MessagePublisher
     */
    default Mono<MessagePublisher> asPublisher() {
        return isPublisher() ? Mono.just(this).cast(MessagePublisher.class) : Mono.empty();
    }

    /**
     * 尝试转为消息订阅,如果不可行则返回{@link Mono#empty()}
     *
     * @return MessageSubscriber
     * @see MessageSubscriber
     */
    default Mono<MessageSubscriber> asSubscriber() {
        return isSubscriber() ? Mono.just(this).cast(MessageSubscriber.class) : Mono.empty();
    }
}
