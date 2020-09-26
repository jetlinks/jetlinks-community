package org.jetlinks.community.timeseries;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.ValueObject;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

public interface TimeSeriesData extends ValueObject {

    long getTimestamp();

    Map<String, Object> getData();

    @Override
    default Map<String, Object> values() {
        return getData();
    }

    @Override
    default Optional<Object> get(String name) {
        return Optional.ofNullable(getData().get(name));
    }

    static TimeSeriesData of(Date date, Map<String, Object> data) {
        return of(date == null ? System.currentTimeMillis() : date.getTime(), data);
    }

    static TimeSeriesData of(long timestamp, Map<String, Object> data) {
        return new SimpleTimeSeriesData(timestamp, data);
    }

    default <T> T as(Class<T> type) {
        return FastBeanCopier.copy(getData(), type);
    }
}
