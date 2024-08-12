package org.jetlinks.community.form.validator;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.form.type.BasicFieldTypes;
import org.springframework.util.Assert;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * 范围值校验
 * <p>
 * validatorSpec:
 * {
 * "provider":"range",
 * "configuration":{
 * "classType":"Long",
 * "max":2,
 * "min":1,
 * "inclusiveMax":true,
 * "inclusiveMin":true,
 * "group":"save"
 * }
 * }
 *
 * @author gyl
 * @since 2.2
 */
public class RangeValueValidator<T extends Comparable<T>> extends AbstractValidator {

    private final T max;
    private final T min;

    //max是包含的还是排除的
    private final boolean inclusiveMax;

    //min是包含的还是排除的
    private final boolean inclusiveMin;

    private final Class<T> classType;

    private static final ConvertUtilsBean convertUtils = BeanUtilsBean.getInstance().getConvertUtils();

    public RangeValueValidator(String message,
                               List<String> group,
                               Class<T> classType,
                               Object max,
                               Object min,
                               boolean inclusiveMax,
                               boolean inclusiveMin) {
        super(message, group);
        this.classType = classType;
        this.max = convert(classType, max);
        this.min = convert(classType, min);
        this.inclusiveMax = inclusiveMax;
        this.inclusiveMin = inclusiveMin;
        Assert.isTrue(max != null || min != null, "max and min cannot both be empty");
    }

    @Override
    public String getCode() {
        return "message.value_range_valida_not_passed";
    }


    private T convert(Class<T> classType, Object extremumObj) {
        if (extremumObj == null) {
            return null;
        }
        return convertUtils.lookup(classType).convert(classType, extremumObj);
    }

    public Result doValidate(Object checked) {
        if (checked != null) {
            T checkedValue = convert(classType, checked);
            if (maxValidate(checkedValue) && minValidate(checkedValue)) {
                return Result.pass();
            }
        }
        return Result.notPass();
    }


    public boolean maxValidate(T checkedValue) {
        if (max == null) {
            return true;
        }
        return inclusiveMax ? checkedValue.compareTo(max) <= 0 : checkedValue.compareTo(max) < 0;
    }

    public boolean minValidate(T checkedValue) {
        if (min == null) {
            return true;
        }
        return inclusiveMin ? checkedValue.compareTo(min) >= 0 : checkedValue.compareTo(min) > 0;
    }

    public static <T extends Comparable<T>> RangeValueValidator<T> ofConfig(Map<String, Object> configuration) {
        BasicFieldTypes classType = BasicFieldTypes.valueOf(String.valueOf(configuration.get("classType")));
        Class<?> javaType = classType.getJavaType();
        if (Comparable.class.isAssignableFrom(javaType)) {
            boolean inclusiveMax = Boolean.parseBoolean(String.valueOf(configuration.getOrDefault("inclusiveMax", true)));
            boolean inclusiveMin = Boolean.parseBoolean(String.valueOf(configuration.getOrDefault("inclusiveMin", true)));
            return new RangeValueValidator<>(DefaultValidatorProvider.getCustomMessage(configuration),
                                             DefaultValidatorProvider.getGroup(configuration),
                                             (Class<T>) javaType,
                                             configuration.get("max"),
                                             configuration.get("min"),
                                             inclusiveMax,
                                             inclusiveMin);
        }
        throw new UnsupportedOperationException("字段类型不支持范围校验：" + javaType.getSimpleName());
    }


    protected static Tuple5<List<String>, Class<?>, Object, Boolean, String> ofSingleConfig(Map<String, Object> configuration) {
        BasicFieldTypes classType = BasicFieldTypes.valueOf(String.valueOf(configuration.get("classType")));
        Class<?> javaType = classType.getJavaType();
        if (Comparable.class.isAssignableFrom(javaType)) {
            boolean inclusive = Boolean.parseBoolean(String.valueOf(configuration.getOrDefault("inclusive", true)));
            List<String> goupList = DefaultValidatorProvider.getGroup(configuration);
            Object value = configuration.get("value");
            String customMessage = Optional
                .ofNullable(DefaultValidatorProvider.getCustomMessage(configuration))
                .orElse(StringUtils.EMPTY);
            return Tuples.of(goupList,
                             javaType,
                             value,
                             inclusive,
                             customMessage);
        }
        throw new UnsupportedOperationException("字段类型不支持范围校验：" + javaType.getSimpleName());
    }


}
