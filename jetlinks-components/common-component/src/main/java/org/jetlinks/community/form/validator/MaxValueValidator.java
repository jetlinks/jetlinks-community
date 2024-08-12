package org.jetlinks.community.form.validator;

import reactor.util.function.Tuple5;

import java.util.List;
import java.util.Map;

/**
 * 最大值校验
 * <p>
 * validatorSpec:
 * {
 * "provider":"max",
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
public class MaxValueValidator<T extends Comparable<T>> extends RangeValueValidator<T> {

    public MaxValueValidator(List<String> group,
                             Class<T> classType,
                             Object value,
                             boolean inclusive,
                             String message) {
        super(message, group, classType, value, null, inclusive, true);
    }

    @Override
    public String getCode() {
        return "message.value_max_valida_not_passed";
    }


    public static <T extends Comparable<T>> MaxValueValidator<T> of(Map<String, Object> configuration) {
        Tuple5<List<String>, Class<?>, Object, Boolean, String> config = ofSingleConfig(configuration);
        return new MaxValueValidator<>(config.getT1(),
                                       (Class<T>) config.getT2(),
                                       config.getT3(),
                                       config.getT4(),
                                       config.getT5());
    }


}
