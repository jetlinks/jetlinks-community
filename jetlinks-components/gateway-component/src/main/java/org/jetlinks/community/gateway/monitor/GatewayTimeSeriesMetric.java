package org.jetlinks.community.gateway.monitor;

import org.jetlinks.community.timeseries.TimeSeriesMetric;

public interface GatewayTimeSeriesMetric {

    String deviceGatewayMetric = "device_gateway_monitor";

    static TimeSeriesMetric deviceGatewayMetric(){
        return TimeSeriesMetric.of(deviceGatewayMetric);
    }
}
