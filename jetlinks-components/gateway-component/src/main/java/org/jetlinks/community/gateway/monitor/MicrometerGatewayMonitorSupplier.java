package org.jetlinks.community.gateway.monitor;

import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.springframework.stereotype.Component;

@Component
public class MicrometerGatewayMonitorSupplier implements MessageGatewayMonitorSupplier, DeviceGatewayMonitorSupplier {

    private final MeterRegistryManager meterRegistryManager;

    public MicrometerGatewayMonitorSupplier(MeterRegistryManager meterRegistryManager) {
        this.meterRegistryManager = meterRegistryManager;
        GatewayMonitors.register((MessageGatewayMonitorSupplier) this);
        GatewayMonitors.register((DeviceGatewayMonitorSupplier) this);

    }

    @Override
    public MessageGatewayMonitor getMessageGatewayMonitor(String id, String... tags) {
        return new MicrometerMessageGatewayMonitor(meterRegistryManager.getMeterRegister(GatewayTimeSeriesMetric.messageGatewayMetric, "target", "connector"), id, tags);
    }

    @Override
    public DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags) {
        return new MicrometerDeviceGatewayMonitor(meterRegistryManager.getMeterRegister(GatewayTimeSeriesMetric.deviceGatewayMetric, "target"), id, tags);
    }
}
