package org.jetlinks.community.utils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class TimeUtils {


    /**
     * 将时间字符解析为{@link Duration}.如: 1d, 15m, 1h15m.
     * 支持天(D,d),时(H,h),分(M,m),秒(s),毫秒(S)
     *
     * @param timeString 时间字符串
     * @return Duration
     */
    public static Duration parse(String timeString) {

        char[] all = timeString.toCharArray();
        if ((all[0] == 'P') || (all[0] == '-' && all[1] == 'P')) {
            return Duration.parse(timeString);
        }
        Duration duration = Duration.ofSeconds(0);
        char[] tmp = new char[32];
        int numIndex = 0;
        for (char c : all) {
            if (c == '-' || (c >= '0' && c <= '9')) {
                tmp[numIndex++] = c;
                continue;
            }
            long val = new BigDecimal(tmp, 0, numIndex).longValue();
            numIndex = 0;
            Duration plus = null;
            if (c == 'D' || c == 'd') {
                plus = Duration.ofDays(val);
            } else if (c == 'H' || c == 'h') {
                plus = Duration.ofHours(val);
            } else if (c == 'M' || c == 'm') {
                plus = Duration.ofMinutes(val);
            } else if (c == 's') {
                plus = Duration.ofSeconds(val);
            } else if (c == 'S') {
                plus = Duration.ofMillis(val);
            } else if (c == 'W' || c == 'w') {
                plus = Duration.ofDays(val * 7);
            }
            if (plus != null) {
                duration = duration.plus(plus);
            }
        }
        return duration;
    }

    public static ChronoUnit parseUnit(String expr) {

        expr = expr.toUpperCase();

        if (!expr.endsWith("S")) {
            expr = expr + "S";
        }

        return ChronoUnit.valueOf(expr);

    }

    /**
     * 将字符串格式化为时间,支持简单的数学运算
     *
     * <pre>
     *     now+1d
     *
     *
     * </pre>
     *
     * @param expr 时间表达式
     * @return 时间
     */
    public static Date parseDate(String expr) {
        return new Date(DateMathParser.parse(expr, System::currentTimeMillis));
    }

}
