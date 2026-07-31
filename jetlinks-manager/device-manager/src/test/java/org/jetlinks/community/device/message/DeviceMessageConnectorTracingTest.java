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
package org.jetlinks.community.device.message;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.supports.event.InternalEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceMessageConnectorTracingTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";
    private static final String TRACE_PARENT = "00-" + TRACE_ID + "-" + SPAN_ID + "-01";

    private InMemorySpanExporter exporter;
    private SdkTracerProvider tracerProvider;
    private DeviceMessageConnector connector;

    @BeforeEach
    void setUp() {
        exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider
            .builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
        OpenTelemetry telemetry = OpenTelemetry
            .propagating(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
        telemetry = io.opentelemetry.sdk.OpenTelemetrySdk
            .builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(telemetry.getPropagators())
            .build();
        TraceHolder.setup(telemetry);
        TraceHolder.enable();

        DeviceRegistry registry = mock(DeviceRegistry.class);
        when(registry.getDevice(anyString())).thenReturn(Mono.empty());

        MessageHandler messageHandler = mock(MessageHandler.class);
        when(messageHandler.reply(any())).thenReturn(Mono.empty());

        DeviceSessionManager sessionManager = mock(DeviceSessionManager.class);
        when(sessionManager.listenEvent(any())).thenReturn(Disposables.disposed());

        connector = new DeviceMessageConnector(
            new InternalEventBus(),
            registry,
            messageHandler,
            sessionManager
        );
    }

    @AfterEach
    void tearDown() {
        TraceHolder.setup(null);
        tracerProvider.shutdown().join(10, TimeUnit.SECONDS);
    }

    @Test
    void shouldWriteCurrentTraceContextToPublishedMessage() {
        ReportPropertyMessage message = new ReportPropertyMessage();
        message.setDeviceId("device-1");
        message.setProperties(Collections.singletonMap("temperature", 25));

        Span source = tracerProvider
            .get("device-message-test")
            .spanBuilder("decode")
            .startSpan();
        try {
            Context context = Context.root().with(source);
            Boolean handled = connector
                .handleMessage(null, message)
                .contextWrite(ctx -> ctx.put(Context.class, context))
                .block();

            assertEquals(Boolean.TRUE, handled);
            assertEquals(
                "00-" + source.getSpanContext().getTraceId() + "-"
                    + source.getSpanContext().getSpanId() + "-01",
                message.getHeader("traceparent").orElse(null)
            );
        } finally {
            source.end();
        }
    }

    @Test
    void shouldCreateResponseSpanFromMessageHeaders() {
        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();
        reply.setDeviceId("device-1");
        reply.setMessageId("message-1");
        reply.setProperties(Collections.singletonMap("temperature", 25));
        reply.addHeader("traceparent", TRACE_PARENT);

        assertEquals(Boolean.TRUE, connector.handleMessage(null, reply).block());

        SpanData response = exporter
            .getFinishedSpanItems()
            .stream()
            .filter(span -> "/device/device-1/response".equals(span.getName()))
            .findFirst()
            .orElse(null);

        assertNotNull(response);
        assertEquals(TRACE_ID, response.getTraceId());
        assertEquals(SPAN_ID, response.getParentSpanId());
        assertEquals(
            reply.toString(),
            String.valueOf(response.getAttributes().get(DeviceTracer.SpanKey.message))
        );
        assertTrue(response.getSpanContext().getTraceFlags().equals(TraceFlags.getSampled()));
        assertEquals(TraceState.getDefault(), response.getSpanContext().getTraceState());
    }
}
