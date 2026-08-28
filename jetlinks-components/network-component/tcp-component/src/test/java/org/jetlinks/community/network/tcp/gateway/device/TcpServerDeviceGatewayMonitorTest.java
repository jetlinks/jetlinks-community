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
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceInfo;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.supports.device.session.LocalDeviceSessionManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TcpServerDeviceGatewayMonitorTest {

    @Test
    void manualAndReturnedTcpUpstreamShouldUseMonitorChainOnce() throws InterruptedException {
        String gatewayId = "tcp-monitor-test";
        RecordingMonitor monitor = new RecordingMonitor();
        GatewayMonitors.register((id, tags) -> gatewayId.equals(id) ? monitor : null);

        DeviceMessageCodec codec = mock(DeviceMessageCodec.class);
        ProtocolSupport protocol = new MockProtocolSupport() {
            @Nonnull
            @Override
            public Mono<DeviceMessageCodec> getMessageCodec(Transport transport) {
                return Mono.just(codec);
            }
        };
        DeviceRegistry registry = new InMemoryDeviceRegistry();
        String productId = "monitor-test-product";
        Mono<Void> registerDevices = registry
            .register(ProductInfo.builder().id(productId).protocol("test").build())
            .then(registry.register(DeviceInfo
                                        .builder()
                                        .id("manual-device")
                                        .productId(productId)
                                        .build()))
            .then(registry.register(DeviceInfo
                                        .builder()
                                        .id("returned-device")
                                        .productId(productId)
                                        .build()))
            .then();
        StepVerifier.create(registerDevices).verifyComplete();
        DeviceSessionManager sessionManager = LocalDeviceSessionManager.create();
        DecodedClientMessageHandler messageHandler = (deviceOperator, message) -> Mono.just(true);
        TcpServer server = mock(TcpServer.class);
        TcpClient client = mock(TcpClient.class);
        ReportPropertyMessage manualMessage = new ReportPropertyMessage();
        manualMessage.setDeviceId("manual-device");
        ReportPropertyMessage returnedMessage = new ReportPropertyMessage();
        returnedMessage.setDeviceId("returned-device");
        TcpMessage origin = new TcpMessage(Unpooled.EMPTY_BUFFER);
        Sinks.Many<TcpClient> clients = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<TcpMessage> messages = Sinks.many().replay().all();

        when(codec.decode(any())).thenAnswer(invocation -> {
            FromDeviceMessageContext context = invocation.getArgument(0);
            return context
                .handleMessage(manualMessage)
                .thenMany(Flux.just(returnedMessage));
        });
        when(server.handleConnection()).thenReturn(clients.asFlux());
        when(client.subscribe()).thenReturn(messages.asFlux());
        when(client.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 1883));
        when(client.getId()).thenReturn("tcp-client");

        TcpServerDeviceGateway gateway = new TcpServerDeviceGateway(
            gatewayId,
            Mono.just(protocol),
            registry,
            messageHandler,
            sessionManager,
            server
        );
        StepVerifier.create(gateway.startup()).verifyComplete();

        assertEquals(Sinks.EmitResult.OK, clients.tryEmitNext(client));
        assertEquals(Sinks.EmitResult.OK, messages.tryEmitNext(origin));
        assertTrue(monitor.completed.await(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS));

        assertEquals(1, monitor.beforeDecode.get());
        assertEquals(1, monitor.decode.get());
        assertEquals(2, monitor.handleUpstream.get());
        assertEquals(2, monitor.beforeSend.get());
        assertEquals(2, monitor.received.get());
        assertEquals(1, Collections.frequency(monitor.monitored, manualMessage));
        assertEquals(1, Collections.frequency(monitor.monitored, returnedMessage));
        assertSame(origin, monitor.origin);

        messages.tryEmitComplete();
        clients.tryEmitComplete();
        StepVerifier.create(gateway.shutdown()).verifyComplete();
    }

    private static class RecordingMonitor implements DeviceGatewayMonitor {
        private final AtomicInteger beforeDecode = new AtomicInteger();
        private final AtomicInteger decode = new AtomicInteger();
        private final AtomicInteger handleUpstream = new AtomicInteger();
        private final AtomicInteger beforeSend = new AtomicInteger();
        private final AtomicInteger received = new AtomicInteger();
        private final CountDownLatch completed = new CountDownLatch(2);
        private final List<DeviceMessage> monitored = new CopyOnWriteArrayList<>();
        private EncodedMessage origin;

        @Override
        public void receivedMessage() {
            received.incrementAndGet();
        }

        @Override
        public boolean beforeDecode(ClientConnection connection, EncodedMessage message) {
            beforeDecode.incrementAndGet();
            origin = message;
            return true;
        }

        @Override
        public Flux<DeviceMessage> decode(ClientConnection connection,
                                          DeviceSession session,
                                          EncodedMessage origin,
                                          Flux<DeviceMessage> decoder) {
            decode.incrementAndGet();
            return decoder;
        }

        @Override
        public Flux<DeviceMessage> handleUpstream(ClientConnection connection,
                                                  DeviceSession session,
                                                  EncodedMessage origin,
                                                  Flux<DeviceMessage> decoder,
                                                  UnaryOperator<Flux<DeviceMessage>> platformHandler) {
            handleUpstream.incrementAndGet();
            return DeviceGatewayMonitor.super
                .handleUpstream(connection, session, origin, decoder, platformHandler);
        }

        @Override
        public Flux<DeviceMessage> beforeSendToPlatform(ClientConnection connection,
                                                        DeviceSession session,
                                                        EncodedMessage origin,
                                                        Flux<DeviceMessage> handler) {
            beforeSend.incrementAndGet();
            return handler.doOnNext(message -> {
                monitored.add(message);
                completed.countDown();
            });
        }
    }
}
