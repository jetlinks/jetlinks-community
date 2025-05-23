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
package org.jetlinks.community.device.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.things.data.ThingEvent;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DeviceEvent extends HashMap<String, Object> {
    private static final long serialVersionUID = 362498820763181265L;

    public DeviceEvent() {

    }

    @Override
    @Hidden
    @JsonIgnore
    public boolean isEmpty() {
        return super.isEmpty();
    }

    public long getTimestamp() {
        return containsKey("timestamp") ? (long) get("timestamp") : 0;
    }

    public DeviceEvent(Map<String, Object> data) {
        super(data);
    }

    public DeviceEvent(ThingEvent data) {
        super(data);
        putIfAbsent("deviceId", data.getThingId());
    }


    @SuppressWarnings("all")
    public void putFormat(EventMetadata metadata) {
        if (metadata != null) {
            DataType type = metadata.getType();
            if (type instanceof ObjectType) {
                Map<String, Object> val = (Map<String, Object>) type.format(this);
                val.forEach((k, v) -> put(k + "_format", v));
            } else {
                put("value_format", type.format(get("value")));
            }
        } else {
            Object value = get("value");
            if (value instanceof Map) {
                ((Map) value).forEach((k, v) -> put(k + "_format", v));
            } else {
                put("value_format", get("value"));
            }
        }
    }
}
