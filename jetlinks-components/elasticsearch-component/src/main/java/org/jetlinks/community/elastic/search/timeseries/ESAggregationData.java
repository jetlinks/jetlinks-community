package org.jetlinks.community.elastic.search.timeseries;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.timeseries.query.AggregationData;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/

@Getter
@AllArgsConstructor
public class ESAggregationData implements AggregationData {

    private Map<String, Object> map;

    @Override
    public Map<String, Object> asMap() {
        return this.map;
    }
}
