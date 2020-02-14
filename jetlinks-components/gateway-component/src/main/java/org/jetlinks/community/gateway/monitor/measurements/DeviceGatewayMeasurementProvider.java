package org.jetlinks.community.gateway.monitor.measurements;

import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.stereotype.Component;

@Component
public class DeviceGatewayMeasurementProvider extends StaticMeasurementProvider {

    public DeviceGatewayMeasurementProvider(TimeSeriesManager timeSeriesManager) {
        super(GatewayDashboardDefinition.gatewayMonitor, GatewayObjectDefinition.deviceGateway);

        addMeasurement(new DeviceGatewayMeasurement(timeSeriesManager));
    }
}
