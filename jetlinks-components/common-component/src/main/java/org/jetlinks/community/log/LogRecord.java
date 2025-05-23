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
package org.jetlinks.community.log;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.utils.ExceptionUtils;
import org.jetlinks.core.utils.SerializeUtils;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

@Getter
@Setter
public class LogRecord implements Externalizable {
    static final Level[] levels = Level.values();

    private Level level;

    private String message;

    private String errorType;

    private String errorDetail;

    private String traceId;

    private String spanId;

    private long timestamp = System.currentTimeMillis();

    public LogRecord() {
        SpanContext span = Span.current().getSpanContext();
        if (span.isValid()) {
            traceId = span.getTraceId();
            spanId = span.getSpanId();
        }
    }


    public LogRecord withLog(Level level, String message, Object[] args) {
        this.level = level;
        Throwable error = MessageFormatter.getThrowableCandidate(args);

        if (null != error) {
            args = MessageFormatter.trimmedCopy(args);
        }
        String i18nMaybe = LocaleUtils.resolveMessage(message, args);
        // 匹配了国际化
        if (!Objects.equals(message, i18nMaybe)) {
            this.message = i18nMaybe;
        } else {
            this.message = MessageFormatter.arrayFormat(message, args).getMessage();
        }
        if (error != null) {
            errorType = error.getClass().getCanonicalName();
            errorDetail = ExceptionUtils.getStackTrace(error);
        }

        return this;

    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeLong(timestamp);
        out.writeByte(level.ordinal());

        SerializeUtils.writeNullableUTF(message, out);
        SerializeUtils.writeNullableUTF(errorType, out);
        SerializeUtils.writeNullableUTF(errorDetail, out);

        SerializeUtils.writeNullableUTF(spanId, out);
        SerializeUtils.writeNullableUTF(traceId, out);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        timestamp = in.readLong();
        level = levels[in.readByte()];

        message = SerializeUtils.readNullableUTF(in);
        errorType = SerializeUtils.readNullableUTF(in);
        errorDetail = SerializeUtils.readNullableUTF(in);

        spanId = SerializeUtils.readNullableUTF(in);
        traceId = SerializeUtils.readNullableUTF(in);
    }


}