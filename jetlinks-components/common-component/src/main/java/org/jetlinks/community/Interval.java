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
package org.jetlinks.community;

import com.alibaba.fastjson.annotation.JSONType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonDeserialize(using = Interval.IntervalJSONDeserializer.class)
@JSONType(deserializer = Interval.IntervalJSONDeserializer.class)
public class Interval {

    public static final String year = "y";
    public static final String quarter = "q";
    public static final String month = "M";
    public static final String weeks = "w";
    public static final String days = "d";
    public static final String hours = "h";
    public static final String minutes = "m";
    public static final String seconds = "s";
    public static final String millis = "S";

    private BigDecimal number;

    private String expression;

    @Override
    public String toString() {
        return (number) + expression;
    }

    @Generated
    public static Interval ofSeconds(int seconds) {
        return of(seconds, Interval.seconds);
    }

    @Generated
    public static Interval ofDays(int days) {
        return of(days, Interval.days);
    }

    @Generated
    public static Interval ofHours(int hours) {
        return of(hours, Interval.hours);
    }

    @Generated
    public static Interval ofMonth(int month) {
        return of(month, Interval.month);
    }

    @Generated
    public static Interval ofMinutes(int month) {
        return of(month, Interval.minutes);
    }

    @Generated
    public static Interval of(int month, String expression) {
        return new Interval(new BigDecimal(month), expression);
    }

    public static Interval of(String expr) {

        char[] chars = expr.toCharArray();
        int numIndex = 0;
        for (char c : expr.toCharArray()) {
            if (c == '-' || c == '.' || (c >= '0' && c <= '9')) {
                numIndex++;
            } else {
                BigDecimal val = new BigDecimal(chars, 0, numIndex);
                return new Interval(val, expr.substring(numIndex));
            }

        }

        throw new IllegalArgumentException("can not parse interval expression:" + expr);
    }

    public String getDefaultFormat() {
        switch (getExpression()) {
            case year:
                return "yyyy";
            case quarter:
            case month:
                return "yyyy-MM";
            case days:
                return "yyyy-MM-dd";
            case hours:
                return "MM-dd HH";
            case minutes:
                return "MM-dd HH:mm";
            case seconds:
                return "HH:mm:ss";
            default:
                return "yyyy-MM-dd HH:mm:ss";
        }
    }

    public IntervalUnit getUnit() {
        switch (expression) {
            case year:
                return IntervalUnit.YEARS;
            case quarter:
                return IntervalUnit.QUARTER;
            case month:
                return IntervalUnit.MONTHS;
            case weeks:
                return IntervalUnit.WEEKS;
            case days:
                return IntervalUnit.DAYS;
            case hours:
                return IntervalUnit.HOURS;
            case minutes:
                return IntervalUnit.MINUTES;
            case seconds:
                return IntervalUnit.SECONDS;
            case millis:
                return IntervalUnit.MILLIS;
        }

        throw new UnsupportedOperationException("unsupported interval express:" + expression);
    }

    public static class IntervalJSONDeserializer extends JsonDeserializer<Interval> {

        @Override
        @SneakyThrows
        public Interval deserialize(JsonParser jp, DeserializationContext ctxt) {
            JsonNode node = jp.getCodec().readTree(jp);

            String currentName = jp.currentName();
            Object currentValue = jp.getCurrentValue();
            if (currentName == null || currentValue == null) {
                return null;
            }
            return of(node.textValue());
        }

    }

    public static class IntervalJSONSerializer extends JsonSerializer<Interval> {

        @Override
        public void serialize(Interval value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }
    }

    public long toMillis() {
        return getUnit().toMillis(number.intValue());
    }

    /**
     * 对指定的时间戳按周期取整
     *
     * @param timestamp 时间戳
     * @return 取整后的值
     */
    public long round(long timestamp) {
        return getUnit().truncatedTo(timestamp, number.intValue());
    }

    /**
     * 按当前周期对指定的时间范围进行迭代,每次迭代一个周期的时间戳
     *
     * @param from 时间从
     * @param to   时间止
     * @return 迭代器
     */
    public Iterable<Long> iterate(long from, long to) {
        return getUnit().iterate(from, to, number.intValue());
    }

    public <T> Flux<T> generate(long from, long to, Function<Long, T> converter) {
        return Flux
            .fromIterable(iterate(from, to))
            .map(converter);
    }

    public <T> Flux<T> generateWithFormat(long from,
                                          long to,
                                          String pattern,
                                          BiFunction<Long, String, T> converter) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
        return generate(from, to, t -> converter.apply(t, new DateTime(t).toString(formatter)));
    }

}
