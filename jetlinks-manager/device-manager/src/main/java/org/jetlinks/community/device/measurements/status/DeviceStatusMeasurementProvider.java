package org.jetlinks.community.device.measurements.status;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.community.device.measurements.DeviceObjectDefinition;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Component
public class DeviceStatusMeasurementProvider extends StaticMeasurementProvider {

    public DeviceStatusMeasurementProvider(MeterRegistryManager registryManager,
                                           TimeSeriesManager timeSeriesManager,
                                           MessageGateway messageGateway) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.status);

        addMeasurement(new DeviceStatusChangeMeasurement(timeSeriesManager, messageGateway));

        MeterRegistry registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId());
        //上线
        messageGateway.subscribe("/device/*/online")
            .window(Duration.ofSeconds(5))
            .flatMap(Flux::count)
            .subscribe(total -> registry
                .counter("online")
                .increment(total));
        //下线
        messageGateway.subscribe("/device/*/offline")
            .window(Duration.ofSeconds(5))
            .flatMap(Flux::count)
            .subscribe(total -> registry
                .counter("offline")
                .increment(total));
    }
}
