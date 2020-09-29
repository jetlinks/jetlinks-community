/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jetlinks.community.utils;


import lombok.SneakyThrows;
import org.hswebframework.utils.time.DateFormatter;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.function.LongSupplier;


public class DateMathParser {


    @SneakyThrows
    public static long parse(String text, LongSupplier now) {
        long time;
        String mathString;
        if (text.startsWith("now()")) {
            time = now.getAsLong();
            mathString = text.substring("now()".length());
        } else if (text.startsWith("now")) {
            time = now.getAsLong();
            mathString = text.substring("now".length());
        } else {
            int index = text.indexOf("||");
            if (index == -1) {
                return parseDateTime(text);
            }
            time = parseDateTime(text.substring(0, index));
            mathString = text.substring(index + 2);
        }

        return parseMath(mathString, time);
    }

    private static long parseMath(final String mathString, final long time) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        for (int i = 0; i < mathString.length(); ) {
            char c = mathString.charAt(i++);
            final boolean round;
            final int sign;
            if (c == '/') {
                round = true;
                sign = 1;
            } else {
                round = false;
                if (c == '+') {
                    sign = 1;
                } else if (c == '-') {
                    sign = -1;
                } else {
                    throw new IllegalArgumentException("不支持的表达式:" + mathString);
                }
            }

            if (i >= mathString.length()) {
                throw new IllegalArgumentException("不支持的表达式:" + mathString);
            }

            final int num;
            if (!Character.isDigit(mathString.charAt(i))) {
                num = 1;
            } else {
                int numFrom = i;
                while (i < mathString.length() && Character.isDigit(mathString.charAt(i))) {
                    i++;
                }
                if (i >= mathString.length()) {
                    throw new IllegalArgumentException("不支持的表达式:" + mathString);
                }
                num = Integer.parseInt(mathString.substring(numFrom, i));
            }
            if (round) {
                if (num != 1) {
                    throw new IllegalArgumentException("不支持的表达式:" + mathString);
                }
            }
            char unit = mathString.charAt(i++);
            switch (unit) {
                case 'y':
                    if (round) {
                        dateTime = dateTime.withDayOfYear(1).with(LocalTime.MIN);
                    } else {
                        dateTime = dateTime.plusYears(sign * num);
                    }

                    break;
                case 'M':
                    if (round) {
                        dateTime = dateTime.withDayOfMonth(1).with(LocalTime.MIN);
                    } else {
                        dateTime = dateTime.plusMonths(sign * num);
                    }

                    break;
                case 'w':
                    if (round) {
                        dateTime = dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
                    } else {
                        dateTime = dateTime.plusWeeks(sign * num);
                    }

                    break;
                case 'd':
                    if (round) {
                        dateTime = dateTime.with(LocalTime.MIN);
                    } else {
                        dateTime = dateTime.plusDays(sign * num);
                    }

                    break;
                case 'h':
                case 'H':
                    if (round) {
                        dateTime = dateTime.withMinute(0).withSecond(0).withNano(0);
                    } else {
                        dateTime = dateTime.plusHours(sign * num);
                    }

                    break;
                case 'm':
                    if (round) {
                        dateTime = dateTime.withSecond(0).withNano(0);
                    } else {
                        dateTime = dateTime.plusMinutes(sign * num);
                    }

                    break;
                case 's':
                    if (round) {
                        dateTime = dateTime.withNano(0);
                    } else {
                        dateTime = dateTime.plusSeconds(sign * num);
                    }

                    break;
                default:
                    throw new IllegalArgumentException("不支持的表达式:" + mathString);
            }
        }
        return dateTime.toInstant().toEpochMilli();
    }

    private static long parseDateTime(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("cannot parse empty date");
        }
        return DateTimeType.GLOBAL.convert(value).getTime();
    }
}
