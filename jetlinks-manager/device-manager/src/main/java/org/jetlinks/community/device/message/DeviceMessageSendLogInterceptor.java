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
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.PropertyMetadataConstants;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 发送设备指令的时候,将消息推送到网关中.
 *
 * @author zhouhao
 * @since 1.1
 */
@Component
@Slf4j
@AllArgsConstructor
public class DeviceMessageSendLogInterceptor implements DeviceMessageSenderInterceptor {

    private final EventBus eventBus;

    private final DeviceRegistry registry;

    public Mono<Void> doPublish(Message message) {
        message.addHeader(PropertyConstants.uid, IDGenerator.RANDOM.generate());
        return DeviceMessageConnector
            .createDeviceMessageTopic(registry, message)
            .flatMap(topic -> eventBus.publish(topic, message))
            .then()
            ;
    }

    private Mono<DeviceMessage> convertParameterType(DeviceOperator device, FunctionInvokeMessage message) {
        if (message.getHeader(Headers.force).orElse(false)) {
            return Mono.just(message);
        }
        return device
            .getMetadata()
            .doOnNext(metadata -> {
                FunctionMetadata function = metadata
                    .getFunction(message.getFunctionId())
                    .orElseThrow(() -> new DeviceOperationException(ErrorCode.FUNCTION_UNDEFINED, "功能[" + message
                        .getFunctionId() + "]未定义"));
                Map<String, FunctionParameter> parameters = message
                    .getInputs()
                    .stream()
                    .collect(Collectors.toMap(FunctionParameter::getName, Function.identity()));
                message.addHeaderIfAbsent(Headers.async, function.isAsync());
                for (PropertyMetadata input : function.getInputs()) {
                    FunctionParameter parameter = parameters.get(input.getId());
                    if (parameter == null || parameter.getValue() == null) {
                        continue;
                    }
                    ValidateResult result = input.getValueType().validate(parameter.getValue());
                    parameter.setValue(result.assertSuccess());
                }
            })
            .thenReturn(message);
    }

    private Mono<DeviceMessage> convertProperty(DeviceOperator device, WritePropertyMessage message) {
        if (message.getHeader(Headers.force).orElse(false)) {
            return Mono.just(message);
        }
        Map<String, Object> properties = new LinkedHashMap<>(message.getProperties());
        //手动写值的属性则直接返回
        return device
            .getMetadata()
            .doOnNext(metadata -> {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    PropertyMetadata propertyMetadata = metadata.getPropertyOrNull(entry.getKey());
                    if (propertyMetadata == null || entry.getValue() == null) {
                        continue;
                    }
                    entry.setValue(propertyMetadata
                        .getValueType()
                        .validate(entry.getValue())
                        .assertSuccess());
                    if (properties.size() == 1) {
                        if (PropertyMetadataConstants.Source.isManual(propertyMetadata)) {
                            //标记手动回复
                            message.addHeader(
                                PropertyMetadataConstants.Source.headerKey, PropertyMetadataConstants.Source.manual
                            );
                        }
                    }
                }
                message.setProperties(properties);
            })
            .thenReturn(message);
    }

    protected Mono<DeviceMessage> prepareMessage(DeviceOperator device, DeviceMessage message) {
        if (message instanceof FunctionInvokeMessage) {
            return convertParameterType(device, ((FunctionInvokeMessage) message));
        }
        if (message instanceof WritePropertyMessage) {
           return convertProperty(device, (WritePropertyMessage) message);
        }
        return Mono.just(message);
    }

    @Override
    public <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {
        //属性来源是手动
        if (PropertyMetadataConstants.Source.isManual(message)) {
            if (message instanceof WritePropertyMessage) {
                WritePropertyMessageReply messageReply = ((WritePropertyMessage) message).newReply();
                PropertyMetadataConstants.Source.setManual(messageReply);
                ((WritePropertyMessage) message).getProperties().forEach(messageReply::addProperty);
                //推送到事件总线然后进行回复
                return doPublish(messageReply)
                    .thenMany(Flux.just(messageReply))
                    .map(r -> (R) r);
            }
        }
        return reply;
    }

    @Override
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        if (message instanceof RepayableDeviceMessage) {
            return this
                .prepareMessage(device, message)
                .flatMap(msg -> this.doPublish(msg).thenReturn(msg));
        } else {
            return Mono.just(message);
        }
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE+1000;
    }
}
