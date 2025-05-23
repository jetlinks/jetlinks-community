/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
