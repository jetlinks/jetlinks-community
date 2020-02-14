package org.jetlinks.community.device.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.dashboard.ObjectDefinition;

@Getter
@AllArgsConstructor
public enum DeviceObjectDefinition implements ObjectDefinition {
    status("设备状态"),
    message("设备消息");

    @Override
    public String getId() {
        return name();
    }

    private String name;
}
