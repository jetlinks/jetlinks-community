package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DeviceLatestData extends HashMap<String,Object> {

    public DeviceLatestData(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public DeviceLatestData(int initialCapacity) {
        super(initialCapacity);
    }

    public DeviceLatestData() {
    }

    public DeviceLatestData(Map<? extends String, ?> m) {
        super(m);
    }

    @Schema(description = "设备ID")
    public String getDeviceId(){
        return (String)Optional.ofNullable(get("deviceId")).orElseGet(()->get("id"));
    }


    @Schema(description = "设备名称")
    public String getDeviceName(){
        return (String)get("deviceName");
    }
}
