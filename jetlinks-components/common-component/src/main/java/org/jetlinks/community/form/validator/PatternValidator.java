package org.jetlinks.community.form.validator;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则校验
 * <p>
 * validatorSpec:
 * {
 * "provider":"pattern",
 * "configuration":{
 * "regexp":"正则表达式"
 * }
 * }
 *
 * @author gyl
 * @since 2.2
 */
public class PatternValidator extends AbstractValidator {

    private final Pattern pattern;

    public PatternValidator(String message, List<String> group, String regexp) {
        super(message, group);
        Assert.isTrue(StringUtils.hasText(regexp), "regex cannot be empty");
        this.pattern = Pattern.compile(regexp);
    }

    @Override
    public String getCode() {
        return "message.value_must_conform_regular_check";
    }

    public Result doValidate(Object value) {
        if (value != null) {
            String strValue = String.valueOf(value);
            Matcher matcher = pattern.matcher(strValue);
            if (matcher.matches()) {
                return Result.pass();
            }
        }
        return Result.notPass();
    }


}
