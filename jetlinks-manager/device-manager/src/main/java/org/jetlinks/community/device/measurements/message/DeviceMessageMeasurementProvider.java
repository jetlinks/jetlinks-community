package org.jetlinks.community.device.measurements.message;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.community.device.measurements.DeviceObjectDefinition;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.message.DeviceMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class DeviceMessageMeasurementProvider extends StaticMeasurementProvider {

    MeterRegistry registry;

    public DeviceMessageMeasurementProvider(MessageGateway messageGateway,
                                            MeterRegistryManager registryManager,
                                            TimeSeriesManager timeSeriesManager) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.message);
        registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId(),
            "target", "msgType", "productId");

        addMeasurement(new DeviceMessageMeasurement(messageGateway, timeSeriesManager));

    }

    @Subscribe("/device/*/message/**")
    public Mono<Void> incrementMessage(TopicMessage message) {
        return Mono.fromRunnable(() -> {
            registry
                .counter("message-count", convertTags(message))
                .increment();
        });
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
