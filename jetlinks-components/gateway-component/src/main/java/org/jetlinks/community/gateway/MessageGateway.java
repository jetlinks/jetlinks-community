package org.jetlinks.community.gateway;

import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于topic对消息网关,用于对各种消息进行路由和转发.
 * <p>
 * 如: 使用 CoAP 发送 /group/1/user/1 消息到网关,其他任何订阅了此topic的客户端将都收到此消息.
 *
 * @author zhouhao
 * @see 1.0
 * @see org.jetlinks.core.event.EventBus
 */
@Deprecated
public interface MessageGateway {

    /**
     * @return 网关ID
     */
    String getId();

    String getName();

    /**
     * 向网关推送消息,消息将根据{@link TopicMessage#getTopic()}发送到对应的订阅者.
     *
     * @param message      消息
     * @param shareCluster 是否广播到集群其他节点
     * @return 成功推送的会话流
     */
    Flux<ClientSession> publish(TopicMessage message, boolean shareCluster);

    /**
     * 向网关推送消息.
     *
     * @param topic        话题
     * @param payload      消息内容
     * @param shareCluster 是否广播到集群其他节点
     * @return 成功推送的会话流
     * @see this#publish(TopicMessage, boolean)
     */
    default Flux<ClientSession> publish(String topic, Object payload, boolean shareCluster) {
        return publish(TopicMessage.of(topic, payload), shareCluster);
    }

    default Flux<ClientSession> publish(String topic, Object payload) {
        return publish(topic, payload, false);
    }

    default Flux<ClientSession> publish(TopicMessage message) {
        return publish(message, false);
    }

    /**
     * 订阅当前网关收到的消息.
     *
     * @param subscription 订阅信息
     * @param shareCluster 是否共享集群中其他节点到消息
     * @return 信息流
     */
    Flux<TopicMessage> subscribe(Collection<Subscription> subscription, boolean shareCluster);

    /**
     * 订阅当前网关收到的消息.
     * 如果存在集群,不会收到来自集群其他节点的消息.
     *
     * @param topics 话题数组,可同时订阅多个话题
     * @return 消息
     */
    default Flux<TopicMessage> subscribe(String... topics) {
        return subscribe(Stream.of(topics).map(Subscription::new).collect(Collectors.toList()), false);
    }

    Flux<TopicMessage> subscribe(Collection<Subscription> subscription, String id, boolean shareCluster);


    /**
     * 注册一个消息连接器,用于进行真实的消息收发
     *
     * @param connector 连接器
     */
    void registerMessageConnector(MessageConnector connector);

    /**
     * 根据连接器ID删除一个连接器
     *
     * @param connectorId 连接器ID {@link MessageConnector#getId()}
     * @return 被删除的连接器, 连接器不存在则返回<code>null</code>
     * @see MessageConnector
     */
    MessageConnector removeConnector(String connectorId);

    /**
     * 启动网关
     */
    void startup();

    /**
     * 停止网关
     */
    void shutdown();

    String nextSubscriberId(String prefix);
}
