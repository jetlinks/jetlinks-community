package org.jetlinks.community.auth.captcha;

import java.util.Optional;

public interface ValidationContext {
    Optional<Object> getParameter(String name);

}
