package org.jetlinks.community.timeseries;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
class SimpleTimeSeriesMetric implements TimeSeriesMetric {

    private String id;

    private String name;

}
