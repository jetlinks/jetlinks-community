package org.jetlinks.community.gateway;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;

import java.util.Map;
import java.util.Optional;

public class DeviceMessageUtils {

    @SuppressWarnings("all")
    public static Optional<DeviceMessage> convert(TopicMessage message){
        Object nativeMessage = message.convertMessage();
        if (nativeMessage instanceof DeviceMessage) {
            return Optional.of((DeviceMessage)nativeMessage);
        } else if (nativeMessage instanceof Map) {
            return MessageType.convertMessage(((Map<String, Object>) nativeMessage));
        }
        return Optional.empty();
    }

}
