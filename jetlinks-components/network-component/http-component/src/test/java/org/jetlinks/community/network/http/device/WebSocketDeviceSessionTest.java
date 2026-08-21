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

import io.netty.buffer.Unpooled;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.http.server.WebSocketExchange;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.DeviceSession;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketDeviceSessionTest {

    @Test
    void downstreamMonitorShouldWrapSender() {
        WebSocketExchange exchange = mock(WebSocketExchange.class);
        WebSocketMessage message = DefaultWebSocketMessage.of(
            WebSocketMessage.Type.TEXT, Unpooled.EMPTY_BUFFER);
        RuntimeException error = new RuntimeException("rejected by monitor");
        AtomicInteger senderSubscriptions = new AtomicInteger();
        AtomicInteger monitorCalls = new AtomicInteger();

        when(exchange.send(message)).thenReturn(Mono.defer(() -> {
            senderSubscriptions.incrementAndGet();
            return Mono.empty();
        }));

        DeviceGatewayMonitor monitor = new DeviceGatewayMonitor() {
            @Override
            public Mono<Void> downstream(ClientConnection connection,
                                         DeviceSession session,
                                         EncodedMessage origin,
                                         Mono<Void> sender) {
                assertSame(exchange, connection);
                assertSame(message, origin);
                monitorCalls.incrementAndGet();
                return Mono.error(error);
            }
        };
        WebSocketDeviceSession session = new WebSocketDeviceSession(monitor, null, exchange);

        StepVerifier
            .create(session.send(message))
            .expectErrorSatisfies(actual -> assertSame(error, actual))
            .verify();

        assertEquals(1, monitorCalls.get());
        assertEquals(0, senderSubscriptions.get());
    }

    @Test
    void httpSessionWebSocketShouldUseDownstreamMonitor() {
        DeviceOperator operator = mock(DeviceOperator.class);
        WebSocketExchange exchange = mock(WebSocketExchange.class);
        WebSocketMessage message = DefaultWebSocketMessage.of(
            WebSocketMessage.Type.TEXT, Unpooled.EMPTY_BUFFER);
        RuntimeException error = new RuntimeException("rejected by monitor");
        AtomicInteger senderSubscriptions = new AtomicInteger();
        AtomicInteger monitorCalls = new AtomicInteger();

        when(operator.getDeviceId()).thenReturn("device-1");
        when(exchange.isAlive()).thenReturn(true);
        when(exchange.send(message)).thenReturn(Mono.defer(() -> {
            senderSubscriptions.incrementAndGet();
            return Mono.empty();
        }));

        DeviceGatewayMonitor monitor = new DeviceGatewayMonitor() {
            @Override
            public Mono<Void> downstream(ClientConnection connection,
                                         DeviceSession session,
                                         EncodedMessage origin,
                                         Mono<Void> sender) {
                assertSame(exchange, connection);
                assertSame(message, origin);
                monitorCalls.incrementAndGet();
                return Mono.error(error);
            }
        };
        HttpDeviceSession session = new HttpDeviceSession(monitor, operator, null);
        session.setWebsocket(exchange);

        StepVerifier
            .create(session.send(message))
            .expectErrorSatisfies(actual -> assertSame(error, actual))
            .verify();

        assertEquals(1, monitorCalls.get());
        assertEquals(0, senderSubscriptions.get());
    }
}
