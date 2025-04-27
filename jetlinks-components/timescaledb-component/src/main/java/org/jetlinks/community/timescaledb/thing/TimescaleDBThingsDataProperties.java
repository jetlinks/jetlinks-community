package org.jetlinks.community.timescaledb.thing;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.Interval;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "timescaledb.things-data")
public class TimescaleDBThingsDataProperties {
    private boolean enabled = true;

    /**
     * 分区时间间隔
     */
    private Interval chunkTimeInterval = Interval.ofDays(7);

    /**
     * 数据保留时长
     */
    private Interval retentionPolicy;
}
