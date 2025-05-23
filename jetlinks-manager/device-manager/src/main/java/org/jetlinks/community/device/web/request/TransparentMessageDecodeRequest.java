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

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.message.DirectDeviceMessage;

import jakarta.validation.constraints.NotBlank;
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
