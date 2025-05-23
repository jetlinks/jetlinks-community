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

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@ConfigurationProperties(prefix = "trace")
@Getter
@Setter
public class TraceProperties {

    private boolean enabled = true;

    private Set<String> ignoreSpans;

    //记录跟踪信息到Jaeger
    private Jaeger jaeger;

    //打印跟踪信息到日志
    private Logging logging = new Logging();


    public void setIgnoreSpans(Set<String> ignoreSpans) {
        this.ignoreSpans = ignoreSpans;
        for (String ignoreSpan : ignoreSpans) {
            TraceHolder.disable(ignoreSpan, "sys-conf");
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            TraceHolder.enable();
        } else {
            TraceHolder.disable();
        }
        this.enabled = enabled;
    }

    public List<SpanProcessor> buildProcessors() {
        List<SpanProcessor> processors = new ArrayList<>();
        if (jaeger != null && jaeger.isEnabled()) {
            processors.add(jaeger.create());
        }
        return processors;
    }

    @Getter
    @Setter
    public static class Logging {
        private String name = "jetlinks.trace";

        public SpanProcessor create() {
            return SimpleSpanProcessor.create(
                LoggingSpanExporter.create(name)
            );
        }
    }

    //https://www.jaegertracing.io/docs/1.18/opentelemetry/
    public static class Jaeger extends GrpcProcessor {
        @Override
        protected SpanExporter createExporter() {
            return JaegerGrpcSpanExporter
                .builder()
                .setEndpoint(getEndpoint())
                .setTimeout(getTimeout())
                .build();
        }
    }

    @Getter
    @Setter
    public abstract static class GrpcProcessor extends BatchProcessor {
        private String endpoint;
        private Duration timeout = Duration.ofSeconds(5);
    }

    @Getter
    @Setter
    public abstract static class BatchProcessor {
        private boolean enabled = true;
        private String endpoint;
        private int maxBatchSize = 2048;
        private int maxQueueSize = 512;
        private Duration exporterTimeout = Duration.ofSeconds(30);
        private Duration scheduleDelay = Duration.ofMillis(100);

        protected abstract SpanExporter createExporter();

        public SpanProcessor create() {
            return BatchSpanProcessor
                .builder(createExporter())
                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                .setMaxExportBatchSize(maxBatchSize)
                .setMaxQueueSize(maxQueueSize)
                .setExporterTimeout(exporterTimeout)
                .build();
        }
    }
}
