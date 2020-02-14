package org.jetlinks.community.device.measurements;

import org.jetlinks.community.dashboard.Dashboard;
import org.jetlinks.community.dashboard.DashboardDefinition;

public interface DeviceDashboard extends Dashboard {


    @Override
    default DashboardDefinition getDefinition() {
        return DeviceDashboardDefinition.instance;
    }
}
