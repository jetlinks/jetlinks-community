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
