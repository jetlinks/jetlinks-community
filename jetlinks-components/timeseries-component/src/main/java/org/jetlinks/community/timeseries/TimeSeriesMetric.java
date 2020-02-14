package org.jetlinks.community.timeseries;

public interface TimeSeriesMetric {

    String getId();

    String getName();

    static TimeSeriesMetric of(String id){
        return of(id,id);
    }

    static TimeSeriesMetric of(String id, String name){
        return new SimpleTimeSeriesMetric(id,name);
    }
}
