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
package org.jetlinks.community.device.web.request;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.MessageEncodeContext;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.util.Optional;

@Getter
@Setter
public class ProtocolEncodePayload {

    private DefaultTransport transport;

    private String payload;

    private PayloadType payloadType = PayloadType.STRING;

    @Generated
    public Message toDeviceMessage() {
        return Optional
            .ofNullable(payload)
            .map(JSON::parseObject)
            .<Message>flatMap(MessageType::convertMessage)
            .orElseThrow(() -> new IllegalArgumentException("error.unrecognized_message"));
    }

    public Flux<Object> doEncode(ProtocolSupport support, DeviceOperator operator) {
        return support
            .getMessageCodec(getTransport())
            .flatMapMany(codec -> codec.encode(new MessageEncodeContext() {
                @Override
                public Message getMessage() {
                    return toDeviceMessage();
                }

                @Nullable
                @Override
                public DeviceOperator getDevice() {
                    return operator;
                }
            }))
            .map(msg -> {
                if (msg instanceof MqttMessage) {
                    JSONObject obj = (JSONObject) JSON.toJSON(msg);
                    obj.put("payload", payloadType.read(msg.getPayload()));
                    obj.remove("bytes");
                    return obj;
                }
                return getPayloadType().read(msg.getPayload());
            });
    }
}
