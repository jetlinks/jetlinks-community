package org.jetlinks.community;

import com.alibaba.fastjson.annotation.JSONType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.math.BigDecimal;

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

    private BigDecimal number;

    private String expression;

    public boolean isFixed() {
        return expression.equalsIgnoreCase(hours) ||
            expression.equals(minutes) ||
            expression.equals(seconds);
    }

    public boolean isCalendar() {
        return expression.equals(days) ||
            expression.equals(month) ||
            expression.equals(year);
    }

    @Override
    public String toString() {
        return (number) + expression;
    }

    public static Interval ofSeconds(int seconds) {
        return of(seconds, Interval.seconds);
    }

    public static Interval ofDays(int days) {
        return of(days, Interval.days);
    }

    public static Interval ofHours(int hours) {
        return of(hours, Interval.hours);
    }

    public static Interval ofMonth(int month) {
        return of(month, Interval.month);
    }

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

}
