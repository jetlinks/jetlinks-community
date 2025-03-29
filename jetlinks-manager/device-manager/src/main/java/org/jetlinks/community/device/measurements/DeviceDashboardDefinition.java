package org.jetlinks.community.device.measurements;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.community.dashboard.DashboardDefinition;

@Getter
@AllArgsConstructor
@Generated
public enum DeviceDashboardDefinition implements DashboardDefinition {

    device("设备信息"),
    product("产品信息");


    private final String name;

    @Override
    public String getId() {
        return name();
    }
}
