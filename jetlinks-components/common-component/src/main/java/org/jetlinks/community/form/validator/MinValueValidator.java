package org.jetlinks.community.form.validator;

import reactor.util.function.Tuple5;

import java.util.List;
import java.util.Map;

/**
 * 最小值校验
 * <p>
 * validatorSpec:
 * {
 * "provider":"min",
 * "configuration":{
 * "classType":"Long",
 * "value":2,
 * "inclusive":false,
 * "group":"save"
 * }
 * }
 *
 * @author gyl
 * @since 2.2
 */
public class MinValueValidator<T extends Comparable<T>> extends RangeValueValidator<T> {

    public MinValueValidator(List<String> group,
                             Class<T> classType,
                             Object value,
                             boolean inclusive,
                             String message) {
        super(message,group, classType, null, value, true, inclusive);
    }

    @Override
    public String getCode() {
        return "message.value_min_valida_not_passed";
    }

    public static <T extends Comparable<T>> MinValueValidator<T> of(Map<String, Object> configuration) {
        Tuple5<List<String>, Class<?>, Object, Boolean, String> config = ofSingleConfig(configuration);
        return new MinValueValidator<>(config.getT1(),
                                       (Class<T>) config.getT2(),
                                       config.getT3(),
                                       config.getT4(),
                                       config.getT5());
    }

}
