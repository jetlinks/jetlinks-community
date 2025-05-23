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
package org.jetlinks.community.network.manager.web.request;

import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.rule.engine.executor.PayloadType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MqttMessageRequest {

    @Generated
    private String topic;

    @Generated
    private int qosLevel;

    @Generated
    private Object data;

    @Generated
    private int messageId;

    @Generated
    private boolean will;

    @Generated
    private boolean dup;

    @Generated
    private boolean retain;

    public static MqttMessage of(MqttMessageRequest request, PayloadType type) {
        SimpleMqttMessage message = FastBeanCopier.copy(request, new SimpleMqttMessage());
        message.setPayload(type.write(request.getData()));
        return message;
    }

    public static MqttMessageRequest of(MqttMessage message, PayloadType type) {
        MqttMessageRequest requestMessage = new MqttMessageRequest();
        requestMessage.setTopic(message.getTopic());
        requestMessage.setQosLevel(message.getQosLevel());
        requestMessage.setData(type.read(message.getPayload()));
        requestMessage.setWill(message.isWill());
        requestMessage.setDup(message.isDup());
        requestMessage.setRetain(message.isRetain());
        requestMessage.setMessageId(message.getMessageId());
        return requestMessage;
    }

}
