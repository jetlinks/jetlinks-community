package org.jetlinks.community.dashboard;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimpleMeasurementValue implements MeasurementValue {

    private Object value;

    private String timeString;

    private long timestamp;

}
