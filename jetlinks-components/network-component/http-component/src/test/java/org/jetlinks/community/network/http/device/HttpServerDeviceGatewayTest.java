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
package org.jetlinks.community.network.http.device;

import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.http.server.HttpExchange;
import org.jetlinks.community.network.http.server.HttpRequest;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.route.HttpRoute;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpServerDeviceGatewayTest {

    @Test
    void httpUpstreamShouldUseDecodeMonitors() throws InterruptedException {
        String gatewayId = "http-monitor-test";
        RecordingMonitor monitor = new RecordingMonitor();
        GatewayMonitors.register((id, tags) -> gatewayId.equals(id) ? monitor : null);

        HttpServer server = mock(HttpServer.class);
        DeviceRegistry registry = mock(DeviceRegistry.class);
        DeviceSessionManager sessionManager = mock(DeviceSessionManager.class);
        DecodedClientMessageHandler messageHandler = mock(DecodedClientMessageHandler.class);
        ProtocolSupport protocol = mock(ProtocolSupport.class);
        DeviceMessageCodec codec = mock(DeviceMessageCodec.class);
        HttpRoute route = mock(HttpRoute.class);
        HttpExchange exchange = mock(HttpExchange.class);
        HttpExchangeMessage message = mock(HttpExchangeMessage.class);
        HttpRequest request = mock(HttpRequest.class);
        Sinks.Many<HttpExchange> requests = Sinks.many().unicast().onBackpressureBuffer();

        when(route.getMethod()).thenReturn(new HttpMethod[]{HttpMethod.POST});
        when(route.getAddress()).thenReturn("/test");
        when(protocol.getRoutes(DefaultTransport.HTTP)).thenReturn(Flux.just(route));
        when(protocol.getRoutes(DefaultTransport.WebSocket)).thenReturn(Flux.empty());
        when(protocol.getMessageCodec(DefaultTransport.HTTP))
            .thenAnswer(ignore -> Mono.just(codec));
        when(codec.decode(any())).thenReturn(Flux.empty());
        when(server.handleRequest(HttpMethod.POST, "/test")).thenReturn(requests.asFlux());
        when(exchange.toExchangeMessage()).thenReturn(Mono.just(message));
        when(exchange.request()).thenReturn(request);
        when(request.getPath()).thenReturn("/test");
        when(exchange.ok()).thenReturn(Mono.empty());

        HttpServerDeviceGateway gateway = new HttpServerDeviceGateway(
            gatewayId,
            server,
            Mono.just(protocol),
            sessionManager,
            registry,
            messageHandler
        );
        StepVerifier.create(gateway.startup()).verifyComplete();

        assertEquals(Sinks.EmitResult.OK, requests.tryEmitNext(exchange));
        assertTrue(monitor.completed.await(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS));

        assertEquals(1, monitor.beforeDecode.get());
        assertEquals(1, monitor.decode.get());
        assertEquals(1, monitor.beforeSend.get());
        assertSame(message, monitor.origin);

        StepVerifier.create(gateway.shutdown()).verifyComplete();
    }

    private static class RecordingMonitor implements DeviceGatewayMonitor {
        private final AtomicInteger beforeDecode = new AtomicInteger();
        private final AtomicInteger decode = new AtomicInteger();
        private final AtomicInteger beforeSend = new AtomicInteger();
        private final CountDownLatch completed = new CountDownLatch(1);
        private EncodedMessage origin;

        @Override
        public boolean beforeDecode(ClientConnection connection, EncodedMessage message) {
            assertNull(connection);
            beforeDecode.incrementAndGet();
            origin = message;
            return true;
        }

        @Override
        public Flux<DeviceMessage> decode(ClientConnection connection,
                                          DeviceSession session,
                                          EncodedMessage origin,
                                          Flux<DeviceMessage> decoder) {
            assertNull(connection);
            decode.incrementAndGet();
            return decoder;
        }

        @Override
        public Flux<DeviceMessage> beforeSendToPlatform(ClientConnection connection,
                                                        DeviceSession session,
                                                        EncodedMessage origin,
                                                        Flux<DeviceMessage> handler) {
            assertNull(connection);
            beforeSend.incrementAndGet();
            completed.countDown();
            return handler;
        }
    }
}
