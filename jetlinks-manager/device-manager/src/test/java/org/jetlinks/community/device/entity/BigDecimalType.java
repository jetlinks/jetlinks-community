package org.jetlinks.community.device.entity;

import org.jetlinks.core.metadata.types.*;

import java.math.BigDecimal;

public class BigDecimalType extends NumberType<BigDecimal> {
    public static final String ID = "big";

    public static final BigDecimalType GLOBAL = new BigDecimalType();
    private static final int SCALE = Integer.getInteger("jetlinks.type.int.scale", 0);


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "big整型";
    }

    @Override
    protected BigDecimal castNumber(Number number) {
        return new BigDecimal(number.intValue());
    }

    @Override
    public int defaultScale() {
        return SCALE;
    }
}
