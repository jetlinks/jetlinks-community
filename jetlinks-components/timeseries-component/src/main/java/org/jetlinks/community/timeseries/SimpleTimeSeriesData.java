package org.jetlinks.community.timeseries;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class SimpleTimeSeriesData implements TimeSeriesData {

    private long timestamp;

    private Map<String,Object> data;


}
