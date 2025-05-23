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
package org.jetlinks.community.things.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.timeseries.TimeSeriesData;

import java.util.HashMap;
import java.util.Map;

@Generated
@Getter
@Setter
public class ThingEvent extends HashMap<String, Object> {

    private String thingId;

    public ThingEvent() {

    }

    public ThingEvent(Map<String, Object> map, String thingIdProperty) {
        super(map);
        this.thingId = (String) map.get(thingIdProperty);
    }

    public ThingEvent(TimeSeriesData data, String thingIdProperty) {
        this(data.getData(), thingIdProperty);
        putIfAbsent(ThingsDataConstants.COLUMN_TIMESTAMP, data.getTimestamp());
    }

    public static ThingEvent of(TimeSeriesData data, String thingIdProperty) {
        return new ThingEvent(data,thingIdProperty);
    }

    @Override
    @Hidden
    @JsonIgnore
    public boolean isEmpty() {
        return super.isEmpty();
    }

    public long getTimestamp() {
        return containsKey(ThingsDataConstants.COLUMN_TIMESTAMP) ? (long) get(ThingsDataConstants.COLUMN_TIMESTAMP) : 0;
    }

    @SuppressWarnings("all")
    public ThingEvent putFormat(EventMetadata metadata) {
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
        return this;
    }
}
