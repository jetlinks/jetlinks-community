package org.jetlinks.community.gateway.monitor;

import org.jetlinks.community.timeseries.TimeSeriesMetric;

public interface GatewayTimeSeriesMetric {

    String deviceGatewayMetric = "device_gateway_monitor";
    String messageGatewayMetric = "message_gateway_monitor";


    static TimeSeriesMetric deviceGatewayMetric(){
        return TimeSeriesMetric.of(deviceGatewayMetric);
    }

    static TimeSeriesMetric messageGatewayMetric(){
        return TimeSeriesMetric.of(messageGatewayMetric);
    }
}
