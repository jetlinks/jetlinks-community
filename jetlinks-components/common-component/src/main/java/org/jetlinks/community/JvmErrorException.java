package org.jetlinks.community;

import org.hswebframework.web.exception.I18nSupportException;

public class JvmErrorException extends I18nSupportException {

    public JvmErrorException(Throwable cause) {
        super("error.jvm_error",cause);
    }


}
