package org.jetlinks.community.device.measurements.status;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.community.device.measurements.DeviceObjectDefinition;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Component
public class DeviceStatusMeasurementProvider extends StaticMeasurementProvider {


    private final MeterRegistry registry;

    public DeviceStatusMeasurementProvider(MeterRegistryManager registryManager,
                                           LocalDeviceInstanceService instanceService,
                                           TimeSeriesManager timeSeriesManager,
                                           EventBus eventBus) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.status);

        addMeasurement(new DeviceStatusChangeMeasurement(timeSeriesManager, eventBus));

        addMeasurement(new DeviceStatusRecordMeasurement(instanceService, timeSeriesManager));

        registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId(),
                                                    "target", "msgType", "productId");
    }

    @Subscribe("/device/*/*/online")
    public Mono<Void> incrementOnline(DeviceMessage msg) {
        return Mono.fromRunnable(() -> {
            String productId = parseProductId(msg);
            registry
                .counter("online", "productId", productId)
                .increment();
        });
    }

    @Subscribe("/device/*/*/offline")
    public Mono<Void> incrementOffline(DeviceMessage msg) {
        return Mono.fromRunnable(() -> {
            String productId = parseProductId(msg);
            registry
                .counter("offline", "productId", productId)
                .increment();
        });
    }

    private String parseProductId(DeviceMessage msg) {
        return msg
            .getHeader("productId")
            .map(String::valueOf)
            .orElse("unknown");
    }
}
