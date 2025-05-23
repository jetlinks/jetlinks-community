/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
