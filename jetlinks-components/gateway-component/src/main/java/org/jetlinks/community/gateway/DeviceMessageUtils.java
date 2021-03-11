package org.jetlinks.community.gateway;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class DeviceMessageUtils {

    public static Optional<DeviceMessage> convert(TopicPayload message) {
        return Optional.of(message.decode(DeviceMessage.class));
    }

    public static Optional<DeviceMessage> convert(ByteBuf payload) {

        return MessageType.convertMessage(JSON.parseObject(payload.toString(StandardCharsets.UTF_8)));

    }

    public static Optional<Map<String, Object>> tryGetProperties(DeviceMessage message) {

        if (message instanceof ReportPropertyMessage) {
            return Optional.ofNullable(((ReportPropertyMessage) message).getProperties());
        }

        if (message instanceof ReadPropertyMessageReply) {
            return Optional.ofNullable(((ReadPropertyMessageReply) message).getProperties());
        }
        if (message instanceof WritePropertyMessageReply) {
            return Optional.ofNullable(((WritePropertyMessageReply) message).getProperties());
        }
        return Optional.empty();
    }

}
