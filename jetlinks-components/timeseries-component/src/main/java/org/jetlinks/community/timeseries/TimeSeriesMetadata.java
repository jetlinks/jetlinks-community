package org.jetlinks.community.timeseries;

import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.Arrays;
import java.util.List;

public interface TimeSeriesMetadata {

    TimeSeriesMetric getMetric();

    List<PropertyMetadata> getProperties();

    static TimeSeriesMetadata of(TimeSeriesMetric metric, PropertyMetadata... properties) {
        return new TimeSeriesMetadata() {
            @Override
            public TimeSeriesMetric getMetric() {
                return metric;
            }

            @Override
            public List<PropertyMetadata> getProperties() {
                return Arrays.asList(properties);
            }
        };
    }
}
