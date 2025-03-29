package org.jetlinks.community.device.measurements.status;

import io.micrometer.core.instrument.MeterRegistry;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.community.device.measurements.DeviceObjectDefinition;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.stereotype.Component;

@Component
public class DeviceStatusMeasurementProvider extends StaticMeasurementProvider {

    private final MeterRegistry registry;

    private final DeviceRegistry deviceRegistry;

    public DeviceStatusMeasurementProvider(MeterRegistryManager registryManager,
                                           ReactiveRepository<DeviceInstanceEntity,String> deviceRepository,
                                           TimeSeriesManager timeSeriesManager,
                                           EventBus eventBus,
                                           DeviceRegistry deviceRegistry) {
        super(DeviceDashboardDefinition.device, DeviceObjectDefinition.status);

        addMeasurement(new DeviceStatusChangeMeasurement(timeSeriesManager, eventBus, deviceRegistry));

        addMeasurement(new DeviceStatusRecordMeasurement(deviceRepository, timeSeriesManager));

        registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId());
        this.deviceRegistry = deviceRegistry;
    }

}
