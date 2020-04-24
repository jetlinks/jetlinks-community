package org.jetlinks.community.device.message;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.community.gateway.EncodableMessage;
import org.jetlinks.community.gateway.TopicMessage;

import java.util.Map;
import java.util.Optional;

public class DeviceMessageUtils {

    public static Optional<DeviceMessage> convert(TopicMessage message){
        return org.jetlinks.community.gateway.DeviceMessageUtils.convert(message);
    }

}
