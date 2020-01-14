package org.jetlinks.community.elastic.search.aggreation.bucket;

import lombok.*;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponseSingleValue;

import java.util.List;

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
}
