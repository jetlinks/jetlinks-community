package org.jetlinks.community.utils;

import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.exception.NotFoundException;
import reactor.core.publisher.Mono;

/**
 * 异常处理工具
 *
 * @author wangzheng
 * @since 1.0
 */
public class ErrorUtils {

    public static <T> Mono<T> notFound(String message) {
        return Mono.error(() -> new NotFoundException(message));
    }

    @SafeVarargs
    public static boolean hasException(Throwable e, Class<? extends Throwable>... target) {
        Throwable cause = e;
        while (cause != null) {
            for (Class<? extends Throwable> aClass : target) {
                if (aClass.isInstance(cause)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
