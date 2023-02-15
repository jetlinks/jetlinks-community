package org.jetlinks.community.device.web.request;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.message.DirectDeviceMessage;

import javax.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
@Setter
public class TransparentMessageDecodeRequest extends TransparentMessageCodecRequest {

    // headers:{
    // "topic":"/xxxx",
    // "url":"/xxx"
    // }
    private Map<String, Object> headers;

    @NotBlank
    private String payload;

    @SneakyThrows
    public DirectDeviceMessage toMessage() {
        ValidatorUtils.tryValidate(this);

        DirectDeviceMessage message = new DirectDeviceMessage();
        message.setDeviceId("test");
        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(message::addHeader);
        }
        byte[] data;
        if (payload.startsWith("0x")) {
            data = Hex.decodeHex(payload.substring(2));
        } else {
            data = payload.getBytes(StandardCharsets.UTF_8);
        }
        message.setPayload(data);

        return message;
    }
}
