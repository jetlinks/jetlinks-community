package org.jetlinks.community.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author bestfeng
 */
@Getter
@Setter
@AllArgsConstructor
public class ValidationResult {

    private boolean passed;

    private String reason;


    public static ValidationResult error(String reason) {
        return new ValidationResult(false, reason);
    }

    public static ValidationResult success(String reason) {
        return new ValidationResult(true, reason);
    }

    public static ValidationResult success() {
        return ValidationResult.success("");
    }

    public static ValidationResult of(boolean passed, String reason) {
        return new ValidationResult(passed, reason);
    }

}
