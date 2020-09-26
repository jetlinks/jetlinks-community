package org.jetlinks.community.timeseries.query;


import org.jetlinks.community.ValueObject;

import java.util.Map;
import java.util.Optional;

public interface AggregationData extends ValueObject {

    Map<String, Object> asMap();

    @Override
    default Optional<Object> get(String name) {
        return Optional.ofNullable(asMap().get(name));
    }

    @Override
    default Map<String, Object> values() {
        return asMap();
    }

    static AggregationData of(Map<String, Object> map) {
        return () -> map;
    }
}
