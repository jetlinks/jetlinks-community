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
package org.jetlinks.community.gateway;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.property.*;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class DeviceMessageUtils {

    @SuppressWarnings("all")
    public static Optional<DeviceMessage> convert(TopicPayload message) {
        return Optional.of(message.decode(DeviceMessage.class));
    }

    public static Optional<DeviceMessage> convert(ByteBuf payload) {
        try {
            return MessageType.convertMessage(JSON.parseObject(payload.toString(StandardCharsets.UTF_8)));
        } finally {
            ReferenceCountUtil.safeRelease(payload);
        }
    }

    public static void trySetProperties(DeviceMessage message, Map<String, Object> properties) {
        if (message instanceof ReportPropertyMessage) {
            ((ReportPropertyMessage) message).setProperties(properties);
        } else if (message instanceof ReadPropertyMessageReply) {
            ((ReadPropertyMessageReply) message).setProperties(properties);
        } else if (message instanceof WritePropertyMessageReply) {
            ((WritePropertyMessageReply) message).setProperties(properties);
        }
    }

    public static Optional<Map<String, Object>> tryGetProperties(DeviceMessage message) {

        if (message instanceof PropertyMessage) {
            return Optional.ofNullable(((PropertyMessage) message).getProperties());
        }

        return Optional.empty();
    }

    public static Optional<Map<String, Long>> tryGetPropertySourceTimes(DeviceMessage message) {
        if (message instanceof PropertyMessage) {
            return Optional.ofNullable(((PropertyMessage) message).getPropertySourceTimes());
        }
        return Optional.empty();
    }

    public static Optional<Map<String, String>> tryGetPropertyStates(DeviceMessage message) {
        if (message instanceof PropertyMessage) {
            return Optional.ofNullable(((PropertyMessage) message).getPropertyStates());
        }
        return Optional.empty();
    }

    public static List<Property> tryGetCompleteProperties(DeviceMessage message) {

        if (message instanceof PropertyMessage) {
            return ((PropertyMessage) message).getCompleteProperties();
        }

        return Collections.emptyList();
    }

    public static void trySetDeviceId(DeviceMessage message, String deviceId) {
        if (message instanceof CommonDeviceMessage) {
            ((CommonDeviceMessage) message).setDeviceId(deviceId);
        } else if (message instanceof CommonDeviceMessageReply) {
            ((CommonDeviceMessageReply<?>) message).setDeviceId(deviceId);
        }
    }

    public static void trySetMessageId(DeviceMessage message, Supplier<String> messageId) {
        if (StringUtils.hasText(message.getMessageId())) {
            return;
        }

        if (message instanceof CommonDeviceMessage) {
            ((CommonDeviceMessage) message).setMessageId(messageId.get());
        } else if (message instanceof CommonDeviceMessageReply) {
            ((CommonDeviceMessageReply<?>) message).setMessageId(messageId.get());
        }
    }

}
