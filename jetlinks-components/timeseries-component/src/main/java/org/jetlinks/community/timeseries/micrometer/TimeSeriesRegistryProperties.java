package org.jetlinks.community.timeseries.micrometer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TimeSeriesRegistryProperties extends StepRegistryProperties {
    private List<String> customTagKeys = new ArrayList<>();
}
