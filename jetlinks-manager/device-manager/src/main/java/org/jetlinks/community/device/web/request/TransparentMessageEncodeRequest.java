/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;

import java.util.Map;

@Getter
@Setter
public class TransparentMessageEncodeRequest extends TransparentMessageCodecRequest {

    /**
     * 消息描述：
     * <pre>
     * {
     *   "messageType": "WRITE_PROPERTY",
     *   "properties": { "temperature": 25.5 }
     * }
     * 或
     * {
     *   "messageType": "READ_PROPERTY",
     *   "properties": ["temperature", "humidity"]
     * }
     * </pre>
     */
    private Map<String, Object> message;

    @SuppressWarnings("unchecked")
    public DeviceMessage toDeviceMessage() {
        if (message == null) {
            throw new IllegalArgumentException("message 不能为空");
        }
         return MessageType
             .<DeviceMessage>convertMessage(message)
             .orElseThrow(() -> new IllegalArgumentException("不支持的 messageType: " + message.get("messageType") + "，仅支持 WRITE_PROPERTY / READ_PROPERTY"));
    }
}
