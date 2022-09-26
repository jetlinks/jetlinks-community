package org.jetlinks.community.things.data;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ObjectMappers;

import java.io.Serializable;

@Getter
@Setter
@Generated
public class ThingMessageLog implements Serializable {

    private String id;

    private String thingId;

    private long createTime;

    private long timestamp;

    private ThingLogType type;

    private String content;

    public static ThingMessageLog of(TimeSeriesData data, String thingIdProperty) {
        ThingMessageLog log = data.as(ThingMessageLog.class);
        log.thingId = data.getString(thingIdProperty, log.thingId);
        return log;
    }

    @Override
    public String toString() {
        return ObjectMappers.toJsonString(this);
    }
}
