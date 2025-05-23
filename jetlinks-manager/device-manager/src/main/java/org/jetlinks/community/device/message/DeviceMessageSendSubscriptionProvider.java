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
package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import lombok.Generated;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class DeviceMessageSendSubscriptionProvider implements SubscriptionProvider {

    private final DeviceRegistry registry;

    private final ReactiveRepository<DeviceInstanceEntity, String> deviceRepository;

    @Override
    @Generated
    public String id() {
        return "device-message-sender";
    }

    @Override
    @Generated
    public String name() {
        return "设备消息发送";
    }

    @Override
    @Generated
    public String[] getTopicPattern() {
        return new String[]{
            "/device-message-sender/*/*"
        };
    }

    @Override
    @SuppressWarnings("all")
    public Flux<Message> subscribe(SubscribeRequest request) {

        String topic = request.getTopic();

        Map<String, String> variables = TopicUtils.getPathVariables("/device-message-sender/{productId}/{deviceId}", topic);
        String deviceId = variables.get("deviceId");
        String productId = variables.get("productId");

        //发给所有设备
        if ("*".equals(deviceId)) {
            return deviceRepository
                .createQuery()
                .select(DeviceInstanceEntity::getId)
                .where(DeviceInstanceEntity::getProductId, productId)
                //.and(DeviceInstanceEntity::getState, DeviceState.online)
                .fetch()
                .map(DeviceInstanceEntity::getId)
                .flatMap(id -> doSend(request.getId(), topic, id, new HashMap<>(request.getParameter())));
        }
        return Flux.fromArray(deviceId.split("[,]"))
            .flatMap(id -> doSend(request.getId(), topic, id, new HashMap<>(request.getParameter())));

    }

    public Flux<Message> doSend(String requestId, String topic, String deviceId, Map<String, Object> message) {
        message.putIfAbsent("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
        message.put("deviceId", deviceId);

        RepayableDeviceMessage<?> msg = MessageType
            .convertMessage(message)
            .filter(RepayableDeviceMessage.class::isInstance)
            .map(RepayableDeviceMessage.class::cast)
            .orElseThrow(() -> new UnsupportedOperationException("error.unsupported_message_format"));
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(Mono.error(() -> new DeviceOperationException(ErrorCode.CLIENT_OFFLINE)))
            .flatMapMany(deviceOperator -> deviceOperator
                .messageSender()
                .send(Mono.just(msg)))
            .map(reply -> Message.success(requestId, topic, reply))
            .onErrorResume(error -> {
                DeviceMessageReply reply = msg.newReply();
                if (error instanceof DeviceOperationException) {
                    reply.error(((DeviceOperationException) error).getCode());
                } else {
                    reply.error(error);
                }
                return Mono.just(Message.success(requestId, topic, reply));
            })
            ;
    }
}
