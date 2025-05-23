/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
