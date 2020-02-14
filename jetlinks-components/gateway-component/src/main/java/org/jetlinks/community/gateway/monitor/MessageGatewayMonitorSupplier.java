package org.jetlinks.community.gateway.monitor;

public interface MessageGatewayMonitorSupplier {
      MessageGatewayMonitor getMessageGatewayMonitor(String id, String... tags);

}
