/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import org.jetlinks.community.logging.system.SerializableSystemLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SystemLoggingAppenderTest {

    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";

    private static final String SPAN_ID = "0123456789abcdef";

    @Test
    void shouldAttachCurrentTraceContext() {
        Span span = Span.wrap(SpanContext.create(
            TRACE_ID,
            SPAN_ID,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        ));

        SerializableSystemLog systemLog;
        try (Scope ignored = span.makeCurrent()) {
            systemLog = SystemLoggingAppender.convertLog(createEvent());
        }

        assertEquals(TRACE_ID, systemLog.getTraceId());
        assertEquals(SPAN_ID, systemLog.getSpanId());
    }

    @Test
    void shouldLeaveTraceContextEmptyWithoutCurrentSpan() {
        SerializableSystemLog systemLog = SystemLoggingAppender.convertLog(createEvent());

        assertNull(systemLog.getTraceId());
        assertNull(systemLog.getSpanId());
    }

    private static LoggingEvent createEvent() {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("org.jetlinks.protocol.monitor");
        event.setLevel(Level.INFO);
        event.setMessage("device message decoded");
        event.setThreadName(Thread.currentThread().getName());
        event.setTimeStamp(System.currentTimeMillis());
        event.setCallerData(new StackTraceElement[]{
            new StackTraceElement("org.jetlinks.protocol.TestCodec", "decode", "TestCodec.java", 42)
        });
        return event;
    }
}
