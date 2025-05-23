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
package org.jetlinks.community.things.utils;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.message.ThingMessageReply;
import org.jetlinks.core.message.event.ThingEventMessage;
import org.jetlinks.core.message.function.ThingFunctionInvokeMessageReply;
import org.jetlinks.core.message.property.Property;
import org.jetlinks.core.message.property.PropertyMessage;

import java.util.Map;

@Slf4j
public class ThingsUtils {
    public static final String FUNCTION_OUTPUT_CONTEXT_KEY = "__output";
    public static final String EVENT_DATA_CONTEXT_KEY = "__data";

    @SuppressWarnings("all")
    public static Map<String, Object> messageToContextMap(ThingMessage message) {
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(32);

        if (message instanceof ThingMessageReply) {
            map.put("success", ((ThingMessageReply) message).isSuccess());
        }
        if (message instanceof ThingFunctionInvokeMessageReply) {
            Object output = ((ThingFunctionInvokeMessageReply) message).getOutput();
            if (output instanceof Map) {
                map.putAll(((Map) output));
            } else if (null != output) {
                map.put(FUNCTION_OUTPUT_CONTEXT_KEY, output);
            }
        } else if (message instanceof PropertyMessage) {
            PropertyMessage msg = ((PropertyMessage) message);
            for (Property property : msg.getCompleteProperties()) {
                map.put(property.getProperty(), property.getValue());
                map.put(property.getProperty() + ".timestamp", property.getTimestamp());
                map.put(property.getProperty() + ".state", property.getState());
            }
        } else if (message instanceof ThingEventMessage) {
            Object data = ((ThingEventMessage) message).getData();
            if (data instanceof Map) {
                map.putAll(((Map) data));
            } else if (null != data) {
                map.put(EVENT_DATA_CONTEXT_KEY, data);
            }
        }

        return map;
    }

}
