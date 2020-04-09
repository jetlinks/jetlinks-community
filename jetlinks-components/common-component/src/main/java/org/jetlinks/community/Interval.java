package org.jetlinks.community;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class Interval {

    public static String year = "y";
    public static String quarter = "q";
    public static String month = "M";
    public static String weeks = "w";
    public static String days = "d";
    public static String hours = "h";
    public static String minutes = "m";
    public static String seconds = "s";

    private BigDecimal number;

    private String expression;

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

        char[] number = new char[32];
        int numIndex = 0;
        for (char c : expr.toCharArray()) {
            if (c == '-' || c == '.' || (c >= '0' && c <= '9')) {
                number[numIndex++] = c;
                continue;
            }
            BigDecimal val = new BigDecimal(number, 0, numIndex);
            return new Interval(val, expr.substring(numIndex));
        }

        throw new IllegalArgumentException("can not parse interval expression:" + expr);
    }

}
