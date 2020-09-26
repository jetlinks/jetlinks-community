package org.jetlinks.community.timeseries.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.Interval;

@Getter
@Setter
@NoArgsConstructor
public class TimeGroup extends Group{

    /**
     * 时间分组间隔,如: 1d , 30s
     */
    private Interval interval;

    /**
     * 时序时间返回格式  如 YYYY-MM-dd
     */
    private String format;

    public TimeGroup(Interval interval, String alias, String format) {
        super("timestamp",alias);
        this.interval = interval;
        this.format = format;
    }
}
