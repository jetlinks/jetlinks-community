package org.jetlinks.community.elastic.search.aggreation.bucket;

import lombok.*;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponseSingleValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bucket {

    private String key;

    private String name;

    private long count;

    private String fromAsString;

    private Object from;

    private String toAsString;

    private Object to;

    private MetricsResponseSingleValue sum;

    private MetricsResponseSingleValue valueCount;

    private MetricsResponseSingleValue avg;

    private MetricsResponseSingleValue min;

    private MetricsResponseSingleValue max;

    private List<Bucket> buckets;

    private double toNumber(double number) {
        return (Double.isInfinite(number) || Double.isNaN(number)) ? 0 : number;
    }

    public Map<String, Number> toMap() {
        Map<String, Number> map = new HashMap<>();
        if (this.sum != null) {
            map.put(sum.getName(), toNumber(sum.getValue()));
        }
        if (this.valueCount != null) {
            map.put(valueCount.getName(), toNumber(valueCount.getValue()));
        }
        if (this.avg != null) {
            map.put(avg.getName(), toNumber(avg.getValue()));
        }
        if (this.min != null) {
            map.put(min.getName(), toNumber(min.getValue()));
        }
        if (this.max != null) {
            map.put(max.getName(), toNumber(max.getValue()));
        }
        return map;
    }
}
