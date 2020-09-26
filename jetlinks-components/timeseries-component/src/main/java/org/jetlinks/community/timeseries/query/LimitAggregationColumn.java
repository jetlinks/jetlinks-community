package org.jetlinks.community.timeseries.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LimitAggregationColumn extends AggregationColumn {

    private int limit;

    public LimitAggregationColumn(String property,
                                  String alias,
                                  Aggregation aggregation,
                                  int limit) {
        super(property, alias, aggregation);
        this.limit = limit;
    }
}
