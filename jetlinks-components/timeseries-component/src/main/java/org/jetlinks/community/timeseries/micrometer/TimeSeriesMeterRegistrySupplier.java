package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.NamingConvention;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.community.micrometer.MeterRegistrySupplier;
import org.jetlinks.community.micrometer.NoopMeterRegistry;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TimeSeriesMeterRegistrySupplier implements MeterRegistrySupplier {

    private final TimeSeriesManager timeSeriesManager;

    @Getter
    @Setter
    private String naming = "elastic";

    static Map<String, NamingConvention> namingSupports = new HashMap<>();

    static {
        namingSupports.put("elastic", new ElasticNamingConvention());
    }

    @Getter
    @Setter
    private Map<String, String> tags = new HashMap<>();

    @Getter
    @Setter
    private Set<String> ignore = new HashSet<>();

    @Getter
    @Setter
    private Map<String, TimeSeriesRegistryProperties> metrics = new HashMap<>();

    public TimeSeriesMeterRegistrySupplier(TimeSeriesManager timeSeriesManager) {
        this.timeSeriesManager = timeSeriesManager;
        metrics.put("default", new TimeSeriesRegistryProperties());
    }

    @Override
    public MeterRegistry getMeterRegistry(String metric, String... tagKeys) {
        if (ignore.contains(metric)) {
            return NoopMeterRegistry.INSTANCE;
        }
        TimeSeriesMeterRegistry registry = new TimeSeriesMeterRegistry(
            timeSeriesManager,
            TimeSeriesMetric.of(metric),
            metrics.getOrDefault(metric, metrics.get("default")),
            tags(), tagKeys);
        registry.config().namingConvention(namingSupports.get(naming));
        return registry;
    }

    private Map<String, String> tags() {
        return this.tags;
    }

    @Override
    public MeterRegistry getMeterRegistry(String metric,
                                          Map<String, DataType> tagDefine) {
        if (ignore.contains(metric)) {
            return NoopMeterRegistry.INSTANCE;
        }
        TimeSeriesMeterRegistry registry = new TimeSeriesMeterRegistry(
            timeSeriesManager,
            TimeSeriesMetric.of(metric),
            metrics.getOrDefault(metric, metrics.get("default")),
            tags(),
            tagDefine);
        registry.config().namingConvention(namingSupports.get(naming));
        return registry;
    }
}
