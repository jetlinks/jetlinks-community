package org.jetlinks.community.elastic.search.aggreation.metrics;

import lombok.*;

/**
 * @author bsetfeng
 * @since 1.0
 **/

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetricsResponseSingleValue {

    private double value;

    private String valueAsString;

    private String name;

    public static MetricsResponseSingleValue empty() {
        return new MetricsResponseSingleValue();
    }

//    public double getValue() {
//        if (isDoubleOrFloat(String.valueOf(value))) {
//            BigDecimal b = BigDecimal.valueOf(value);
//            return b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//        } else {
//            return 0;
//        }
//    }
//
//    public static boolean isDoubleOrFloat(String str) {
//        if (str.length() > 15) {
//            str = str.substring(0, 15);
//        }
//        Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");
//        return pattern.matcher(str).matches();
//    }
//
//    public static void main(String[] args) {
//        System.out.println(new BigDecimal("8.839927333333333E7"));
//    }
}
