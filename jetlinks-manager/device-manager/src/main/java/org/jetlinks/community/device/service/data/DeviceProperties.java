package org.jetlinks.community.device.service.data;

import lombok.Getter;
import org.jetlinks.community.things.data.ThingProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
public class DeviceProperties extends HashMap<String, Object> {

    private final String deviceId;

    public DeviceProperties(Map<String, Object> data) {
        super(data);
        this.deviceId = (String) data.get("deviceId");
    }

    public DeviceProperties(ThingProperties data) {
        super(data);
        this.deviceId = (String) data.get("thingId");
    }

}
