package org.jetlinks.community.gateway.monitor;

public interface DeviceGatewayMonitorSupplier {
      DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags);

}
