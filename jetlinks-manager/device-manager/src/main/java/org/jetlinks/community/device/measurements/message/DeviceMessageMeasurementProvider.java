package org.jetlinks.community.device.measurements.message;

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
public class DeviceMessageMeasurementProvider extends StaticMeasurementProvider {

    public DeviceMessageMeasurementProvider(MessageGateway messageGateway,
                                            MeterRegistryManager registryManager,
                                            TimeSeriesManager timeSeriesManager) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.message);
        addMeasurement(new DeviceMessageMeasurement(messageGateway, timeSeriesManager));

        //定时提交设备消息量
        MeterRegistry registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId());

        messageGateway.subscribe("/device/*/message/**")
            .window(Duration.ofSeconds(5))
            .flatMap(Flux::count)
            .subscribe(total -> registry
                .counter("message-count")
                .increment(total));

    }
}
