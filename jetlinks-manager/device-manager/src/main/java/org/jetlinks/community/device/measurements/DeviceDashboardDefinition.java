package org.jetlinks.community.device.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.dashboard.DashboardDefinition;

@Getter
@AllArgsConstructor
public enum DeviceDashboardDefinition implements DashboardDefinition {

    instance("device","设备信息");

   private String id;

   private String name;
}
