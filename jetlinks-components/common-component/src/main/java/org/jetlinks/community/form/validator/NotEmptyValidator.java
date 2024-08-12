package org.jetlinks.community.form.validator;

import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 非空校验
 * <p>
 * validatorSpec:
 * {
 * "provider":"notEmpty"
 * }
 *
 * @author gyl
 * @since 2.2
 */
public class NotEmptyValidator extends AbstractValidator {

    public NotEmptyValidator(String message, List<String> group) {
        super(message, group);
    }

    @Override
    public String getCode() {
        return "message.value_cannot_be_empty";
    }

    public Result doValidate(Object value) {
        if (value != null) {
            if (value instanceof String) {
                return Result.of(StringUtils.hasText(String.valueOf(value)));
            }
            if (value instanceof Collection) {
                return Result.of(!((Collection<?>) value).isEmpty());
            }
        }
        return Result.notPass();
    }


}
