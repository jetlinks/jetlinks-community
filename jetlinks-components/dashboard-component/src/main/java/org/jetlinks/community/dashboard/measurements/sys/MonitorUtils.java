package org.jetlinks.community.dashboard.measurements.sys;

import java.math.BigDecimal;
import java.math.RoundingMode;

class MonitorUtils {

     static float round(float val) {
        return BigDecimal
            .valueOf(val)
            .setScale(1, RoundingMode.HALF_UP)
            .floatValue();
    }

}
