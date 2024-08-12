package org.jetlinks.community.form.validator;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.i18n.LocaleUtils;

import javax.validation.groups.Default;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public interface Validator {

    default Result validate(Object value) {
        return validate(value, Collections.singletonList(Default.class));
    }

    default Result validate(Object value, Class<?>... groups) {
        return validate(value, Arrays.asList(groups));
    }


    Result validate(Object value, Collection<Class<?>> groups);

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    class Result {
        private boolean passed;
        private String code;
        private String message;

        public String getMessage() {
            if (StringUtils.isBlank(this.message) && StringUtils.isNotBlank(this.code)) {
                return this.message = LocaleUtils.resolveMessage(code);
            }
            return this.message;
        }

        public static Result of(boolean passed) {
            return passed ? pass() : notPass();
        }

        public static Result notPass() {
            Result result = new Result();
            result.setPassed(false);
            return result;
        }

        public static Result pass() {
            Result result = new Result();
            result.setPassed(true);
            result.setCode("message.validator_is_passed");
            return result;
        }
    }

}
