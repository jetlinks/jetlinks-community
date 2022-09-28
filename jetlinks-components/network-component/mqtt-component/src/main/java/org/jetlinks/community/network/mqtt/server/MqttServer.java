package org.jetlinks.community.network.mqtt.server;

import org.jetlinks.community.network.ServerNetwork;
import reactor.core.publisher.Flux;

/**
 * MQTT服务端
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface MqttServer extends ServerNetwork {

    /**
     * 订阅客户端连接
     *
     * @return 客户端连接流
     */
    Flux<MqttConnection> handleConnection();

    Flux<MqttConnection> handleConnection(String holder);

}
