package org.jetlinks.community.dashboard;

import lombok.*;
import org.hswebframework.utils.time.DateFormatter;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimpleMeasurementValue implements MeasurementValue {

    private Object value;

    private String timeString;

    private long timestamp;

    public static SimpleMeasurementValue of(Object value, Date time) {
        return of(value, DateFormatter.toString(time, "yyyy-MM-dd HH:mm:ss"), time.getTime());
    }

    public static SimpleMeasurementValue of(Object value, long time) {
        return of(value, new Date(time));
    }
}
