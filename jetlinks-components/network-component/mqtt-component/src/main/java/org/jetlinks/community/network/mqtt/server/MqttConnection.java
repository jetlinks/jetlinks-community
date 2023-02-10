package org.jetlinks.community.network.mqtt.server;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.mqtt.MqttAuth;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * MQTT连接信息,一个MQTT连接就是一个MQTT客户端.
 * <pre>
 *     //先定义处理逻辑
 *     connection
 *        .handleMessage()
 *        .subscribe(msg-> log.info("handle mqtt message:{}",msg));
 *
 *     //再接受连接
 *     connection.accept();
 *
 * </pre>
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface MqttConnection extends ClientConnection {

    /**
     * 获取MQTT客户端ID
     *
     * @return clientId
     */
    String getClientId();

    /**
     * 获取MQTT认证信息
     *
     * @return 可选的MQTT认证信息
     */
    Optional<MqttAuth> getAuth();

    /**
     * 拒绝MQTT连接
     *
     * @param code 返回码
     * @see MqttConnectReturnCode
     */
    void reject(MqttConnectReturnCode code);

    /**
     * 接受连接.接受连接后才能进行消息收发.
     *
     * @return 当前连接信息
     */
    MqttConnection accept();

    /**
     * 获取遗言消息
     *
     * @return 可选的遗言信息
     */
    Optional<MqttMessage> getWillMessage();

    /**
     * 订阅客户端推送的消息
     *
     * @return 消息流
     */
    Flux<MqttPublishing> handleMessage();

    /**
     * 推送消息到客户端
     *
     * @param message MQTT消息
     * @return 异步推送结果
     */
    Mono<Void> publish(MqttMessage message);

    /**
     * 订阅客户端订阅请求
     *
     * @param autoAck 是否自动应答
     * @return 订阅请求流
     */
    Flux<MqttSubscription> handleSubscribe(boolean autoAck);

    /**
     * 订阅客户端取消订阅请求
     *
     * @param autoAck 是否自动应答
     * @return 取消订阅请求流
     */
    Flux<MqttUnSubscription> handleUnSubscribe(boolean autoAck);

    /**
     * 监听断开连接
     *
     * @param listener 监听器
     */
    void onClose(Consumer<MqttConnection> listener);

    /**
     * 获取MQTT连接是否存活,当客户端断开连接或者 客户端ping超时后则返回false.
     *
     * @return mqtt连接是否存活
     */
    boolean isAlive();

    /**
     * 关闭mqtt连接
     *
     * @return 异步关闭结果
     */
    Mono<Void> close();

    long getLastPingTime();

    void keepAlive();

    Duration getKeepAliveTimeout();

    void setKeepAliveTimeout(Duration duration);

    InetSocketAddress getClientAddress();
}
