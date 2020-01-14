package org.jetlinks.community.device.events;

import lombok.Builder;
import lombok.Data;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Builder
@Data
public class GaugePropertyEvent {

    private String propertyName;

    private Object propertyValue;

    private String deviceId;
}
