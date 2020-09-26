package org.jetlinks.community.timeseries.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LimitGroup extends Group {
    private int limit;

    public LimitGroup(String property, String alias, int limit) {
        super(property, alias);
        this.limit = limit;
    }
}
