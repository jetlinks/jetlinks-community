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
package org.jetlinks.community.tracing;

import io.micrometer.context.ThreadLocalAccessor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.ContextStorageProvider;
import io.opentelemetry.context.Scope;
import org.jetlinks.community.configuration.CommonConfiguration;
import org.jetlinks.community.log.LogRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceContextPropagationTest {

    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";

    private static final String SPAN_ID = "0123456789abcdef";

    @BeforeAll
    static void enableAutomaticContextPropagation() {
        new CommonConfiguration().afterPropertiesSet();
    }

    @AfterAll
    static void disableAutomaticContextPropagation() {
        Hooks.disableAutomaticContextPropagation();
    }

    @AfterEach
    void clearCurrentContext() {
        new ThreadLocalContextStorageProvider().setValue();
    }

    @Test
    void shouldRegisterOpenTelemetryAndMicrometerProviders() {
        assertTrue(ServiceLoader
                       .load(ContextStorageProvider.class)
                       .stream()
                       .anyMatch(provider -> provider.type() == ThreadLocalContextStorageProvider.class));
        assertTrue(ServiceLoader
                       .load(ThreadLocalAccessor.class)
                       .stream()
                       .anyMatch(provider -> provider.type() == ThreadLocalContextStorageProvider.class));
    }

    @Test
    void shouldRestoreNestedScopes() {
        Span outer = span(TRACE_ID, SPAN_ID);
        Span inner = span("fedcba9876543210fedcba9876543210", "fedcba9876543210");

        assertFalse(Span.current().getSpanContext().isValid());
        try (Scope ignored = outer.makeCurrent()) {
            assertEquals(outer.getSpanContext(), Span.current().getSpanContext());
            try (Scope nested = inner.makeCurrent()) {
                assertEquals(inner.getSpanContext(), Span.current().getSpanContext());
            }
            assertEquals(outer.getSpanContext(), Span.current().getSpanContext());
        }
        assertFalse(Span.current().getSpanContext().isValid());
    }

    @Test
    void shouldProtectScopeRestorationFromInvalidCloseOrder() {
        ThreadLocalContextStorageProvider provider = new ThreadLocalContextStorageProvider();
        ContextStorage storage = provider.get();
        Context outer = Context.root().with(span(TRACE_ID, SPAN_ID));
        Context inner = Context.root().with(span(
            "fedcba9876543210fedcba9876543210",
            "fedcba9876543210"
        ));

        try (Scope ignored = storage.attach(null)) {
            assertNull(storage.current());
        }

        Scope outerScope = storage.attach(outer);
        try (Scope ignored = storage.attach(outer)) {
            assertEquals(outer, storage.current());
        }
        Scope innerScope = storage.attach(inner);

        outerScope.close();
        assertEquals(inner, storage.current());

        innerScope.close();
        assertEquals(outer, storage.current());

        outerScope.close();
        outerScope.close();
        assertNull(storage.current());
    }

    @Test
    void shouldAttachTraceIdToLogRecordAcrossSchedulerAndClearContext() {
        Scheduler scheduler = Schedulers.newSingle("trace-context-test");
        Context traceContext = Context.root().with(span(TRACE_ID, SPAN_ID));
        try {
            Mono<String> tracedLog = Mono
                .just("decoded")
                .publishOn(scheduler)
                .map(ignored -> new LogRecord().getTraceId())
                .contextWrite(context -> context.put(Context.class, traceContext));

            Mono<Boolean> contextCleared = Mono
                .fromSupplier(() -> Span.current().getSpanContext().isValid())
                .subscribeOn(scheduler);

            StepVerifier
                .create(tracedLog.concatWith(contextCleared.map(String::valueOf)))
                .expectNext(TRACE_ID, "false")
                .verifyComplete();
        } finally {
            scheduler.dispose();
        }
    }

    @Test
    void shouldClearContextAfterError() {
        Scheduler scheduler = Schedulers.newSingle("trace-context-error-test");
        Context traceContext = Context.root().with(span(TRACE_ID, SPAN_ID));
        try {
            Mono<String> failed = Mono
                .just("decoded")
                .publishOn(scheduler)
                .<String>flatMap(ignored -> {
                    assertEquals(TRACE_ID, Span.current().getSpanContext().getTraceId());
                    return Mono.error(new IllegalStateException("expected failure"));
                })
                .contextWrite(context -> context.put(Context.class, traceContext));

            StepVerifier
                .create(failed.onErrorResume(ignored -> Mono
                    .fromSupplier(() -> String.valueOf(Span.current().getSpanContext().isValid()))
                    .subscribeOn(scheduler)))
                .expectNext("false")
                .verifyComplete();
        } finally {
            scheduler.dispose();
        }
    }

    @Test
    void shouldClearContextAfterCancellation() {
        Scheduler scheduler = Schedulers.newSingle("trace-context-cancel-test");
        Context traceContext = Context.root().with(span(TRACE_ID, SPAN_ID));
        try {
            Flux<String> cancellable = Mono
                .just("ready")
                .publishOn(scheduler)
                .map(ignored -> Span.current().getSpanContext().getTraceId())
                .concatWith(Mono.never())
                .contextWrite(context -> context.put(Context.class, traceContext));

            StepVerifier
                .create(cancellable)
                .expectNext(TRACE_ID)
                .thenCancel()
                .verify();

            StepVerifier
                .create(Mono
                            .fromSupplier(() -> Span.current().getSpanContext().isValid())
                            .subscribeOn(scheduler))
                .expectNext(false)
                .verifyComplete();
        } finally {
            scheduler.dispose();
        }
    }

    private static Span span(String traceId, String spanId) {
        return Span.wrap(SpanContext.create(
            traceId,
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        ));
    }
}
