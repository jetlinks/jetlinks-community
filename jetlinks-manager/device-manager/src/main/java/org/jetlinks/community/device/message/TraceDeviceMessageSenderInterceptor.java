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

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.community.utils.ObjectMappers;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TraceDeviceMessageSenderInterceptor implements DeviceMessageSenderInterceptor, Ordered {

    @Override
    @SuppressWarnings("all")
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        //跟踪信息放入header中
        return TraceHolder
            .writeContextTo(message, DeviceMessage::addHeader);
    }

    @Override
    public Flux<DeviceMessage> doSend(DeviceOperator device, DeviceMessage source, Flux<DeviceMessage> sender) {
        return sender
            .as(FluxTracer
                    .create(
                        DeviceTracer.SpanName.request0(device.getDeviceId()),
                        (span, response) -> span
                            .setAttributeLazy(DeviceTracer.SpanKey.response, ()-> ObjectMappers.toJsonString(response.toJson())),
                        builder -> builder
                            .setAttribute(DeviceTracer.SpanKey.deviceId, device.getDeviceId())
                            .setAttributeLazy(DeviceTracer.SpanKey.message, ()->ObjectMappers.toJsonString(source.toJson()))
                    )
            );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
