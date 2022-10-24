package org.jetlinks.community.utils.math;

import org.jetlinks.reactor.ql.utils.CastUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

public class MathUtils {


    public static Number castSimpleNumber(BigDecimal decimal) {
        if (decimal.scale() == 0) {
            //int
            if (decimal.unscaledValue().bitLength() < 32) {
                return decimal.intValue();
            }
            //long
            if (decimal.unscaledValue().bitLength() < 64) {
                return decimal.longValue();
            }
        }
        return decimal;
    }

    public static BigDecimal numberOperation(BiFunction<BigDecimal, BigDecimal, BigDecimal> operator,
                                             Object... params) {
        BigDecimal main = BigDecimal.ZERO;

        for (Object d : params) {
            if (d instanceof Map) {
                d = ((Map<?, ?>) d).values();
            }
            if (d instanceof Collection) {
                main = operator.apply(main, numberOperation(operator, ((Collection<?>) d).toArray()));
                continue;
            }
            main = operator.apply(main, (BigDecimal) CastUtils
                .castNumber(d,
                            i -> BigDecimal.valueOf(i.longValue()),
                            BigDecimal::valueOf,
                            BigDecimal::valueOf,
                            BigDecimal::valueOf,
                            s -> s instanceof BigDecimal ? s : new BigDecimal(s.toString())));
        }

        return main;
    }

}
