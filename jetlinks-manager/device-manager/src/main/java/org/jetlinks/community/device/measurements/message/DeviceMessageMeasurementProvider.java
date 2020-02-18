package org.jetlinks.community.device.measurements.message;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.community.device.measurements.DeviceObjectDefinition;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.message.DeviceMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Component
public class DeviceMessageMeasurementProvider extends StaticMeasurementProvider {

    public DeviceMessageMeasurementProvider(MessageGateway messageGateway,
                                            MeterRegistryManager registryManager,
                                            TimeSeriesManager timeSeriesManager) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.message);
        addMeasurement(new DeviceMessageMeasurement(messageGateway, timeSeriesManager));


        MeterRegistry registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId(),
            "target", "msgType", "productId");

        //订阅设备消息,用于统计设备消息量
        messageGateway.subscribe("/device/*/message/**")
            .map(this::convertTags)
            .subscribe(tags -> registry
                .counter("message-count", tags)
                .increment());

    }

    static final String[] empty = new String[0];

    private String[] convertTags(TopicMessage msg) {
        DeviceMessage message = DeviceMessageUtils.convert(msg).orElse(null);
        if (message == null) {
            return empty;
        }
        return new String[]{
            "msgType", message.getMessageType().name().toLowerCase(),
            "productId", message.getHeader("productId").map(String::valueOf).orElse("unknown")
        };
    }
}
