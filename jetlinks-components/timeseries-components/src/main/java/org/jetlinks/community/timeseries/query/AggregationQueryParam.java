package org.jetlinks.community.timeseries.query;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.QueryParam;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
public class AggregationQueryParam {

    private List<AggregationTerm> aggregations;

    private Duration interval;

    private int limit;

    private long startWithTime;

    private long endWithTime;


    private QueryParam queryParam;

}
