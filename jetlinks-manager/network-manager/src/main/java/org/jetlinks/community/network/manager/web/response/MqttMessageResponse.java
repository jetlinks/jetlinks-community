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
package org.jetlinks.community.network.manager.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.rule.engine.executor.PayloadType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageResponse {

    private int messageId;

    private Object payload;

    private String topic;

    private int qosLevel;

    private boolean dup;


    public static MqttMessageResponse of(MqttMessage mqttMessage, PayloadType type) {
        return MqttMessageResponse.builder()
                .dup(mqttMessage.isDup())
                .payload(type.read(mqttMessage.getPayload()))
                .messageId(mqttMessage.getMessageId())
                .qosLevel(mqttMessage.getQosLevel())
                .topic(mqttMessage.getTopic())
                .build();
    }
}
