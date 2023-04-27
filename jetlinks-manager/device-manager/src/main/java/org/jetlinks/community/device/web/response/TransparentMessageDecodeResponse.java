package org.jetlinks.community.device.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.DeviceMessage;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TransparentMessageDecodeResponse {
    private boolean success;

    private String reason;

    private List<Object> outputs;

    public static TransparentMessageDecodeResponse of(List<DeviceMessage> messages) {
        TransparentMessageDecodeResponse response = new TransparentMessageDecodeResponse();
        response.success = true;
        response.outputs = messages
            .stream()
            .map(DeviceMessage::toJson)
            .collect(Collectors.toList());

        return response;
    }

    public static TransparentMessageDecodeResponse error(String reason) {
        TransparentMessageDecodeResponse response = new TransparentMessageDecodeResponse();
        response.success = false;
        response.reason = reason;
        return response;
    }

    public static TransparentMessageDecodeResponse of(Throwable err) {
        return error(err.getLocalizedMessage());
    }
}
