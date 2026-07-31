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
package org.jetlinks.community.device.message.transparent;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.device.entity.TransparentMessageCodecEntity;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DirectDeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransparentDeviceMessageConnectorTracingTest {

    private InMemorySpanExporter exporter;
    private SdkTracerProvider tracerProvider;
    private DecodedClientMessageHandler messageHandler;
    private TransparentDeviceMessageConnector connector;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider
            .builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
        OpenTelemetry telemetry = OpenTelemetrySdk
            .builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();
        TraceHolder.setup(telemetry);
        TraceHolder.enable();

        TransparentMessageCodecProviders.providers.clear();
        messageHandler = mock(DecodedClientMessageHandler.class);
        when(messageHandler.handleMessage(isNull(), any(ReadPropertyMessage.class)))
            .thenReturn(Mono.empty());

        ObjectProvider<TransparentMessageCodecProvider> providers = mock(ObjectProvider.class);
        when(providers.iterator()).thenReturn(List.<TransparentMessageCodecProvider>of(new TestProvider()).iterator());

        connector = new TransparentDeviceMessageConnector(
            mock(ReactiveRepository.class),
            messageHandler,
            mock(DeviceSessionManager.class),
            mock(DeviceRegistry.class),
            mock(EventBus.class),
            providers
        );
    }

    @AfterEach
    void tearDown() {
        TransparentMessageCodecProviders.providers.clear();
        TraceHolder.setup(null);
        tracerProvider.shutdown().join(10, TimeUnit.SECONDS);
    }

    @Test
    void shouldCreateHandleSpanForTransparentDeviceMessage() {
        TransparentMessageCodecEntity entity = new TransparentMessageCodecEntity();
        entity.setProductId("product-1");
        entity.setProvider("test-provider");
        entity.setConfiguration(Map.of());
        StepVerifier.create(connector.doLoadCodec(entity)).verifyComplete();

        DirectDeviceMessage direct = new DirectDeviceMessage();
        direct.setDeviceId("device-1");
        direct.addHeader(Headers.productId, "product-1");
        StepVerifier.create(connector.handleMessage(direct)).verifyComplete();

        verify(messageHandler).handleMessage(isNull(), any(ReadPropertyMessage.class));
        SpanData handle = exporter
            .getFinishedSpanItems()
            .stream()
            .filter(span -> "/device/device-1/handle".equals(span.getName()))
            .findFirst()
            .orElse(null);

        assertNotNull(handle);
        assertNotNull(handle.getAttributes().get(DeviceTracer.SpanKey.message));
    }

    static class TestProvider implements TransparentMessageCodecProvider {

        @Override
        public String getProvider() {
            return "test-provider";
        }

        @Override
        public Mono<TransparentMessageCodec> createCodec(Map<String, Object> configuration) {
            return Mono.just(new TestCodec());
        }
    }

    static class TestCodec implements TransparentMessageCodec {

        @Override
        public Flux<DeviceMessage> decode(DirectDeviceMessage message) {
            ReadPropertyMessage decoded = new ReadPropertyMessage();
            decoded.setDeviceId(message.getDeviceId());
            decoded.addProperties("temperature");
            return Flux.just(decoded);
        }

        @Override
        public Mono<DirectDeviceMessage> encode(DeviceMessage message) {
            return Mono.empty();
        }
    }
}
