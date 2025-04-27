package org.jetlinks.community.logging.utils;

import org.jetlinks.community.utils.ObjectMappers;
import org.springframework.http.codec.multipart.FilePart;

/**
 * @author gyl
 * @since 2.2
 */
public class LoggingUtil {

    public static Object convertParameterValue(Object value) {
        if (value instanceof FilePart) {
            return ("file:") + ((FilePart) value).filename();
        }
        if (value instanceof Class) {
            return ((Class<?>) value).getName();
        }
        String className = value.getClass().getName();
        if (className.startsWith("org.springframework")) {
            return className;
        }
        if (value instanceof String || value instanceof Number) {
            return value;
        }
        return ObjectMappers.toJsonString(value);
    }


}
