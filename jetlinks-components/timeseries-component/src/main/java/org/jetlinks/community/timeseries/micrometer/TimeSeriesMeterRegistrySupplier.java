package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.micrometer.MeterRegistrySupplier;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

import java.util.HashMap;
import java.util.Map;

public class TimeSeriesMeterRegistrySupplier implements MeterRegistrySupplier {

    private final TimeSeriesManager timeSeriesManager;

    @Getter
    @Setter
    private Map<String, String> tags = new HashMap<>();

    @Getter
    @Setter
    private Map<String, TimeSeriesRegistryProperties> metrics = new HashMap<>();

    public TimeSeriesMeterRegistrySupplier(TimeSeriesManager timeSeriesManager) {
        this.timeSeriesManager = timeSeriesManager;
        metrics.put("default", new TimeSeriesRegistryProperties());
    }

    @Override
    public MeterRegistry getMeterRegistry(String metric) {
        return new TimeSeriesMeterRegistry(timeSeriesManager, TimeSeriesMetric.of(metric), metrics.getOrDefault(metric, metrics.get("default")), tags);
    }

}
