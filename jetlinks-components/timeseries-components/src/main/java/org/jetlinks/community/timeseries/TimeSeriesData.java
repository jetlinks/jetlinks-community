package org.jetlinks.community.timeseries;

import java.util.Map;

public interface TimeSeriesData {

    long getTimestamp();

    Map<String,Object> getData();

}
