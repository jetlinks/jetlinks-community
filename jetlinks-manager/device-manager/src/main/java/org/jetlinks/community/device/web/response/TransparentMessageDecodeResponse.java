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
