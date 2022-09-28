package org.jetlinks.community.network.mqtt.client;

import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.community.network.Network;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * MQTT Client
 *
 * @author zhouhao
 * @since 1.0
 */
public interface MqttClient extends Network {

    /**
     * 从MQTT Broker订阅Topic
     *
     * @param topics topic列表
     * @return MQTT消息流
     */
    default Flux<MqttMessage> subscribe(List<String> topics) {
        return subscribe(topics, 0);
    }

    /**
     * 自定义QoS，从MQTT Broker订阅Topic
     *
     * @param topics topic列表
     * @param qos    QoS
     * @return MQTT消息流
     */
    Flux<MqttMessage> subscribe(List<String> topics, int qos);

    /**
     * 推送MQTT消息到MQTT Broker
     *
     * @param message 消息
     * @return void
     */
    Mono<Void> publish(MqttMessage message);

}
