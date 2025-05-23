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
package org.jetlinks.community.configure.trace;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.AllArgsConstructor;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;

@AllArgsConstructor
final class LoggingSpanExporter implements SpanExporter {
    private final Logger logger;

    public static LoggingSpanExporter create(String name) {
        return new LoggingSpanExporter(LoggerFactory.getLogger(name));
    }

    @Override
    public CompletableResultCode export(@Nonnull Collection<SpanData> spans) {
        if (!logger.isTraceEnabled()) {
            return CompletableResultCode.ofSuccess();
        }
        for (SpanData span : spans) {
            String log = StringBuilderUtils.buildString(span, ((data, sb) -> {
                InstrumentationLibraryInfo instrumentationLibraryInfo = data.getInstrumentationLibraryInfo();
                sb.append("'")
                  .append(data.getName())
                  .append("' : ")
                  .append(data.getTraceId())
                  .append(" ")
                  .append(data.getSpanId())
                  .append(" ")
                  .append(data.getKind())
                  .append(" [tracer: ")
                  .append(instrumentationLibraryInfo.getName())
                  .append(":")
                  .append(
                      instrumentationLibraryInfo.getVersion() == null
                          ? ""
                          : instrumentationLibraryInfo.getVersion())
                  .append("] ")
                  .append(data.getAttributes());
            }));

            logger.trace(log);
        }
        return CompletableResultCode.ofSuccess();
    }

    /**
     * Flushes the data.
     *
     * @return the result of the operation
     */
    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return flush();
    }
}
