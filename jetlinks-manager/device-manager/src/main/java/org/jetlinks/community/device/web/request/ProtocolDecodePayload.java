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
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@Setter
public class ProtocolDecodePayload {

    private DefaultTransport transport;

    private PayloadType payloadType = PayloadType.STRING;

    private String payload;

    @SuppressWarnings("all")
    @Generated
    public EncodedMessage toEncodedMessage() {
        if (transport == DefaultTransport.MQTT || transport == DefaultTransport.MQTT_TLS) {
            if (payload.startsWith("{")) {
                SimpleMqttMessage message = FastBeanCopier.copy(JSON.parseObject(payload), new SimpleMqttMessage());
                message.setPayloadType(MessagePayloadType.of(payloadType.getId()));
            }
            return SimpleMqttMessage.of(payload);
        } else if (transport == DefaultTransport.CoAP || transport == DefaultTransport.CoAP_DTLS) {
            return DefaultCoapMessage.of(payload);
        }

        return EncodedMessage.simple(payloadType.write(payload));
    }

    public Flux<? extends Message> doDecode(ProtocolSupport support, DeviceOperator deviceOperator) {
        return support
            .getMessageCodec(getTransport())
            .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                @Nonnull
                @Override
                public EncodedMessage getMessage() {
                    return toEncodedMessage();
                }

                @Override
                public DeviceSession getSession() {
                    return null;
                }

                @Nullable
                @Override
                public DeviceOperator getDevice() {
                    return deviceOperator;
                }
            }));
    }
}
