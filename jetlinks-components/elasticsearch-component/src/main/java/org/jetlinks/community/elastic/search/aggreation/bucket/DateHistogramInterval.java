
package org.jetlinks.community.elastic.search.aggreation.bucket;

/**
 * The interval the date histogram is based on.
 */
public class DateHistogramInterval {

    public static final String SECOND = "1s";
    public static final String MINUTE = "1m";
    public static final String HOUR = "1h";
    public static final String DAY = "1d";
    public static final String WEEK = "1w";
    public static final String MONTH = "1M";
    public static final String QUARTER = "1q";
    public static final String YEAR = "1y";

    public static String seconds(int sec) {
        return sec + "s";
    }

    public static String minutes(int min) {
        return min + "m";
    }

    public static String hours(int hours) {
        return hours + "h";
    }

    public static String days(int days) {
        return days + "d";
    }

    public static String weeks(int weeks) {
        return weeks + "w";
    }

}
