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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.logging.system.SerializableSystemLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SystemLoggingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    public static ApplicationEventPublisher publisher;

    public static final Map<String, String> staticContext = new ConcurrentHashMap<>();

    @Override
    @Generated
    protected void append(ILoggingEvent event) {
        doPublish(convertLog(event));
    }

    protected static void doPublish(SerializableSystemLog systemLog) {
        if (publisher == null) {
            return;
        }
        try {
            publisher.publishEvent(systemLog);
        } catch (IllegalStateException | BeanCreationNotAllowedException ignore) {
        } catch (Throwable e) {
            log.error("publish system log error", e);
        }
    }

    static SerializableSystemLog convertLog(ILoggingEvent event) {

        StackTraceElement element = event.getCallerData()[0];
        IThrowableProxy proxies = event.getThrowableProxy();
        String message = event.getFormattedMessage();
        String stack = ShortenedThrowableConverter.getStackTrace(proxies);

        Map<String, String> context = new HashMap<>(staticContext);
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        if (mdc != null) {
            context.putAll(mdc);
        }
        SerializableSystemLog info = SerializableSystemLog
            .builder()
            .id(IDGenerator.RANDOM.generate())
            .context(context)
            .name(event.getLoggerName())
            .level(event.getLevel().levelStr)
            .className(element.getClassName())
            .methodName(element.getMethodName())
            .lineNumber(element.getLineNumber())
            .exceptionStack(stack)
            .threadName(event.getThreadName())
            .createTime(event.getTimeStamp())
            .message(message)
            .threadId(String.valueOf(Thread.currentThread().getId()))
            .build();

        return info;

    }
}
