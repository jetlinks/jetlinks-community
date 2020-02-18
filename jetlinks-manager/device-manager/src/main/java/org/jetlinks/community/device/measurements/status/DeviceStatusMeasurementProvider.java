package org.jetlinks.community.device.measurements.status;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.community.device.measurements.DeviceObjectDefinition;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Component
public class DeviceStatusMeasurementProvider extends StaticMeasurementProvider {

    public DeviceStatusMeasurementProvider(MeterRegistryManager registryManager,
                                           LocalDeviceInstanceService instanceService,
                                           TimeSeriesManager timeSeriesManager,
                                           MessageGateway messageGateway) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.status);

        addMeasurement(new DeviceStatusChangeMeasurement(timeSeriesManager, messageGateway));

        addMeasurement(new DeviceStatusRecordMeasurement(instanceService, timeSeriesManager));

        MeterRegistry registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId(),
            "target", "msgType", "productId");
        Map<String, LongAdder> productCounts = new ConcurrentHashMap<>();

        Function<String, LongAdder> counterAdder = productId ->
            productCounts.computeIfAbsent(productId, __id -> {
                LongAdder adder = new LongAdder();
                Gauge.builder("online-count", adder, LongAdder::sum)
                    .tag("productId", __id)
                    .register(registry);
                return adder;
            });

        //上线
        messageGateway.subscribe("/device/*/online")
            .map(this::parseProductId)
            .subscribe(productId -> {
                counterAdder.apply(productId).increment();
                registry
                    .counter("online", "productId", productId)
                    .increment();
            });
        //下线
        messageGateway.subscribe("/device/*/offline")
            .map(this::parseProductId)
            .subscribe(productId -> {
                counterAdder.apply(productId).decrement();
                registry
                    .counter("offline", "productId", productId)
                    .increment();
            });
    }

    private String parseProductId(TopicMessage msg) {
        return DeviceMessageUtils.convert(msg)
            .flatMap(deviceMessage -> deviceMessage.getHeader("productId"))
            .map(String::valueOf).orElse("unknown");
    }
}
