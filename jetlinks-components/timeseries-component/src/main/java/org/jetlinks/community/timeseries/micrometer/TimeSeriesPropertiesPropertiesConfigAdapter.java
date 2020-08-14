package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

class TimeSeriesPropertiesPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<TimeSeriesRegistryProperties>
      implements StepRegistryConfig {

  TimeSeriesPropertiesPropertiesConfigAdapter(TimeSeriesRegistryProperties properties) {
      super(properties);
  }

    @Override
    public String prefix() {
        return "management.metrics.export.time-series";
    }
}
