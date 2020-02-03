package org.jetlinks.community.timeseries.query;

import java.util.Map;

public interface AggregationData {

    String getTimeString();

    Map<String, Number> getData();

}
