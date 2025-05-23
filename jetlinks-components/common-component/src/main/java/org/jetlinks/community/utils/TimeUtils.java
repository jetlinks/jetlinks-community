/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.utils;

import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;

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
        if (numIndex != 0) {
            duration = duration.plus(Duration.ofMillis(new BigDecimal(tmp, 0, numIndex).longValue()));
        }
        return duration;
    }


    public static ChronoUnit parseUnit(String expr) {

        expr = expr.toUpperCase();

        if (expr.equals("MILLENNIA")) {
            return ChronoUnit.MILLENNIA;
        } else if (expr.equals("FOREVER")) {
            return ChronoUnit.FOREVER;
        }
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

    public static Date convertToDate(Object obj) {
        if(obj instanceof String){
            return new Date(DateMathParser.parse(String.valueOf(obj), System::currentTimeMillis));
        }
        return CastUtils.castDate(obj);
    }


    public static long round(long ts, long interval) {
        return (ts / interval) * interval;
    }

    /**
     * 解析指定时间区间为每一个间隔时间
     *
     * @param from     时间从
     * @param to       时间到
     * @param interval 间隔
     * @return 时间
     */
    public static Flux<Long> parseIntervalRange(long from, long to, long interval) {
        return Flux
            .create(sink -> {
                long _from = from, _to = to;
                if (_from > _to) {
                    _from = to;
                    _to = from;
                }
                _from = round(_from, interval);
                _to = round(_to, interval);
                sink.next(_from);
                while (_from < _to && !sink.isCancelled()) {
                    _from = _from + interval;
                    sink.next(_from);
                }
                sink.complete();
            });

    }
}
