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
package org.jetlinks.community.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.jetlinks.core.utils.ExceptionUtils;

public class ShortenedThrowableConverter extends ThrowableHandlingConverter {
    @Override
    public String convert(ILoggingEvent event) {
        return getStackTrace(event.getThrowableProxy());
    }

    public static void writeStackTraceElement(StringBuilder builder,
                                              StackTraceElementProxy[] elements) {
        int unimportantCount = 0;
        for (StackTraceElementProxy element : elements) {
            if (ExceptionUtils.compactEnabled && ExceptionUtils.isUnimportant(element.getStackTraceElement())) {
                unimportantCount++;
                continue;
            }
            if (unimportantCount > 0) {
                builder.append("\t...")
                       .append(unimportantCount)
                       .append(" frames excluded\n");
                unimportantCount = 0;
            }
            builder.append("\t")
                   .append(element)
                   .append("\n");
        }

        if (unimportantCount > 0) {
            builder.append("\t...")
                   .append(unimportantCount)
                   .append(" frames excluded\n");
        }
    }

    public static String getStackTrace(IThrowableProxy e) {
        if (e == null) {
            return "";
        }
        return getStackTrace(new StringBuilder(), e).toString();
    }

    public static StringBuilder getStackTrace(StringBuilder builder,
                                              IThrowableProxy e) {
        if (e instanceof ThrowableProxy) {
            builder
                .append(((ThrowableProxy) e).getThrowable());
        } else {
            builder
                .append(e.getClassName())
                .append(e.getMessage());
        }


        builder.append("\n");

        StackTraceElementProxy[] elements = e.getStackTraceElementProxyArray();
        if (elements != null && elements.length != 0) {
            writeStackTraceElement(builder, elements);
        }

        for (IThrowableProxy throwable : e.getSuppressed()) {
            builder.append("Suppressed: ");
            getStackTrace(builder, throwable);
        }

        IThrowableProxy cause = e.getCause();
        if (cause != null) {
            builder.append("Caused by: ");
            getStackTrace(builder, cause);
        }

        return builder;
    }

}
