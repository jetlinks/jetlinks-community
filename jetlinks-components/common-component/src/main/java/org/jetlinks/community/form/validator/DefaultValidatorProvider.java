package org.jetlinks.community.form.validator;

import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.utils.ConverterUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author gyl
 * @since 2.2
 */
@Getter
public enum DefaultValidatorProvider implements ValidatorProvider {

    range {
        @Override
        public Validator creatValidator(Map<String, Object> configuration) {
            return RangeValueValidator.ofConfig(configuration);
        }
    },
    max {
        @Override
        public Validator creatValidator(Map<String, Object> configuration) {
            return MaxValueValidator.of(configuration);
        }
    },
    min {
        @Override
        public Validator creatValidator(Map<String, Object> configuration) {
            return MinValueValidator.of(configuration);
        }
    },
    pattern {
        @Override
        public Validator creatValidator(Map<String, Object> configuration) {
            String regexp = String.valueOf(configuration.get("regexp"));
            return new PatternValidator(getCustomMessage(configuration), getGroup(configuration), regexp);
        }
    },
    notEmpty {
        @Override
        public Validator creatValidator(Map<String, Object> configuration) {
            return new NotEmptyValidator(getCustomMessage(configuration), getGroup(configuration));
        }
    },
    ;

    @Override
    public String getProvider() {
        return name();
    }

    @Override
    public abstract Validator creatValidator(Map<String, Object> configuration);

    public static void load() {
        //execute static block
    }

    static {
        for (DefaultValidatorProvider value : values()) {
            ValidatorProvider.providers.register(value.getProvider(), value);
        }
    }


    public static List<String> getGroup(Map<String, Object> configuration) {
        if (MapUtils.isEmpty(configuration)) {
            return Collections.emptyList();
        }
        return ConverterUtils.convertToList(configuration.get("group"), String::valueOf);
    }

    public static String getCustomMessage(Map<String, Object> configuration) {
        if (MapUtils.isEmpty(configuration)) {
            return null;
        }
        Object message = configuration.get("message");
        return message == null ? null : String.valueOf(configuration.get("message"));
    }

}
