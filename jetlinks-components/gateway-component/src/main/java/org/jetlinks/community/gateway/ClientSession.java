package org.jetlinks.community.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 客户端连接会话,用于保存客户端订阅信息.
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface ClientSession {

    /**
     * @return 会话ID
     */
    String getId();

    /**
     * @return 客户端ID
     */
    String getClientId();

    /**
     * @return 是否持久化
     */
    boolean isPersist();

    /**
     * 获取会话的全部订阅信息
     *
     * @return 订阅信息
     */
    Flux<Subscription> getSubscriptions();

    /**
     * 添加订阅信息
     *
     * @param subscription 订阅信息
     * @return 添加结果
     */
    Mono<Void> addSubscription(Subscription subscription);

    /**
     * 移除订阅信息
     *
     * @param subscription 订阅信息
     * @return 移除结果
     */
    Mono<Void> removeSubscription(Subscription subscription);

    /**
     * @return 会话是否存活
     */
    boolean isAlive();

    /**
     * 关闭会话
     */
    void close();
}
