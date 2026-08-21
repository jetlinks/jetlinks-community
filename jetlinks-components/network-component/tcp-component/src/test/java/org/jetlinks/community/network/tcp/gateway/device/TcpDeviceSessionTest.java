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
package org.jetlinks.community.network.tcp.gateway.device;

import io.netty.buffer.Unpooled;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
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

class TcpDeviceSessionTest {

    @Test
    void downstreamMonitorShouldWrapSender() {
        DeviceOperator operator = mock(DeviceOperator.class);
        DeviceSessionManager sessionManager = mock(DeviceSessionManager.class);
        TcpClient client = mock(TcpClient.class);
        TcpMessage message = new TcpMessage(Unpooled.EMPTY_BUFFER);
        RuntimeException error = new RuntimeException("rejected by monitor");
        AtomicInteger senderSubscriptions = new AtomicInteger();
        AtomicInteger monitorCalls = new AtomicInteger();

        when(operator.getId()).thenReturn("device-1");
        when(client.isAlive()).thenReturn(true);
        when(client.sendMessage(message)).thenReturn(Mono.defer(() -> {
            senderSubscriptions.incrementAndGet();
            return Mono.empty();
        }));

        DeviceGatewayMonitor monitor = new DeviceGatewayMonitor() {
            @Override
            public Mono<Void> downstream(ClientConnection connection,
                                         DeviceSession session,
                                         EncodedMessage origin,
                                         Mono<Void> sender) {
                assertSame(client, connection);
                assertSame(message, origin);
                monitorCalls.incrementAndGet();
                return Mono.error(error);
            }
        };
        TcpDeviceSession session = new TcpDeviceSession(
            operator, DefaultTransport.TCP, monitor, sessionManager);
        session.registerConnection(client);

        StepVerifier
            .create(session.send(message))
            .expectErrorSatisfies(actual -> assertSame(error, actual))
            .verify();

        assertEquals(1, monitorCalls.get());
        assertEquals(0, senderSubscriptions.get());
    }

    @Test
    void unknownSessionShouldAlsoUseDownstreamMonitor() {
        TcpClient client = mock(TcpClient.class);
        TcpMessage message = new TcpMessage(Unpooled.EMPTY_BUFFER);
        RuntimeException error = new RuntimeException("rejected by monitor");
        AtomicInteger senderSubscriptions = new AtomicInteger();
        AtomicInteger monitorCalls = new AtomicInteger();

        when(client.sendMessage(message)).thenReturn(Mono.defer(() -> {
            senderSubscriptions.incrementAndGet();
            return Mono.empty();
        }));

        DeviceGatewayMonitor monitor = new DeviceGatewayMonitor() {
            @Override
            public Mono<Void> downstream(ClientConnection connection,
                                         DeviceSession session,
                                         EncodedMessage origin,
                                         Mono<Void> sender) {
                assertSame(client, connection);
                assertSame(message, origin);
                monitorCalls.incrementAndGet();
                return Mono.error(error);
            }
        };
        UnknownTcpDeviceSession session = new UnknownTcpDeviceSession(
            "unknown", client, DefaultTransport.TCP, monitor);

        StepVerifier
            .create(session.send(message))
            .expectErrorSatisfies(actual -> assertSame(error, actual))
            .verify();

        assertEquals(1, monitorCalls.get());
        assertEquals(0, senderSubscriptions.get());
    }
}
