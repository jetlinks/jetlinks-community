package org.jetlinks.community.device.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.types.ObjectType;

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
