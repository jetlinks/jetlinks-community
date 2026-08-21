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
package org.jetlinks.community.gateway.monitor;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.DeviceSession;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DeviceGatewayMonitorTest {

    @Test
    void shouldKeepDefaultMonitorCompatibleAndTransparent() {
        AtomicInteger connected = new AtomicInteger();
        AtomicInteger disconnected = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        DeviceGatewayMonitor monitor = new DeviceGatewayMonitor() {
            @Override
            public void connected() {
                connected.incrementAndGet();
            }

            @Override
            public void disconnected() {
                disconnected.incrementAndGet();
            }

            @Override
            public void rejected() {
                rejected.incrementAndGet();
            }
        };

        ClientConnection connection = mock(ClientConnection.class);
        DeviceSession session = mock(DeviceSession.class);
        EncodedMessage origin = mock(EncodedMessage.class);
        Flux<DeviceMessage> decoder = Flux.empty();
        Mono<Void> sender = Mono.empty();

        assertTrue(monitor.connected(connection));
        monitor.disconnected(connection);
        monitor.rejected(connection, null);
        assertTrue(monitor.beforeDecode(connection, origin));
        assertSame(decoder, monitor.decode(connection, session, origin, decoder));
        assertSame(decoder, monitor.beforeSendToPlatform(connection, session, origin, decoder));
        assertSame(sender, monitor.downstream(connection, session, origin, sender));
        assertEquals(1, connected.get());
        assertEquals(1, disconnected.get());
        assertEquals(1, rejected.get());
    }

    @Test
    void shouldComposeAllMonitorDecisionsAndReactiveWrappersInOrder() {
        List<String> signals = new ArrayList<>();
        RecordingMonitor first = new RecordingMonitor("first", true, true, signals);
        RecordingMonitor second = new RecordingMonitor("second", false, false, signals);
        CompositeDeviceGatewayMonitor monitor = new CompositeDeviceGatewayMonitor()
            .add(first, second);

        ClientConnection connection = mock(ClientConnection.class);
        DeviceSession session = mock(DeviceSession.class);
        EncodedMessage origin = mock(EncodedMessage.class);
        DeviceMessage message = mock(DeviceMessage.class);

        assertFalse(monitor.connected(connection));
        assertFalse(monitor.beforeDecode(connection, origin));
        monitor.rejected(connection, new IllegalStateException("rejected"));
        monitor.disconnected(connection);

        StepVerifier
            .create(monitor.beforeSendToPlatform(
                connection,
                session,
                origin,
                monitor.decode(connection, session, origin, Flux.just(message))))
            .expectNext(message)
            .verifyComplete();

        StepVerifier
            .create(monitor.downstream(
                connection,
                session,
                origin,
                Mono.fromRunnable(() -> signals.add("sender"))))
            .verifyComplete();

        assertEquals(
            Arrays.asList(
                "first:connected", "second:connected",
                "first:beforeDecode", "second:beforeDecode",
                "first:rejected", "second:rejected",
                "first:disconnected", "second:disconnected",
                "first:decode", "second:decode",
                "first:beforeSend", "second:beforeSend",
                "sender", "first:downstream", "second:downstream"
            ),
            signals
        );
    }

    @Test
    void shouldLazilyResolveAndDelegateExtendedApi() {
        List<String> signals = new ArrayList<>();
        AtomicInteger resolved = new AtomicInteger();
        RecordingMonitor target = new RecordingMonitor("target", false, false, signals);
        LazyDeviceGatewayMonitor monitor = new LazyDeviceGatewayMonitor(() -> {
            resolved.incrementAndGet();
            return target;
        });

        ClientConnection connection = mock(ClientConnection.class);
        DeviceSession session = mock(DeviceSession.class);
        EncodedMessage origin = mock(EncodedMessage.class);
        DeviceMessage message = mock(DeviceMessage.class);

        assertFalse(monitor.connected(connection));
        assertFalse(monitor.beforeDecode(connection, origin));
        StepVerifier
            .create(monitor.decode(connection, session, origin, Flux.just(message)))
            .expectNext(message)
            .verifyComplete();
        StepVerifier
            .create(monitor.beforeSendToPlatform(connection, session, origin, Flux.just(message)))
            .expectNext(message)
            .verifyComplete();
        StepVerifier
            .create(monitor.downstream(connection, session, origin, Mono.empty()))
            .verifyComplete();

        assertEquals(1, resolved.get());
        assertEquals(
            Arrays.asList(
                "target:connected", "target:beforeDecode",
                "target:decode", "target:beforeSend", "target:downstream"
            ),
            signals
        );
    }

    @Test
    void shouldPropagateReactiveErrorsThroughCompositeMonitor() {
        CompositeDeviceGatewayMonitor monitor = new CompositeDeviceGatewayMonitor()
            .add(
                new RecordingMonitor("first", true, true, new ArrayList<>()),
                new RecordingMonitor("second", true, true, new ArrayList<>())
            );
        ClientConnection connection = mock(ClientConnection.class);
        DeviceSession session = mock(DeviceSession.class);
        EncodedMessage origin = mock(EncodedMessage.class);
        IllegalStateException error = new IllegalStateException("failed");

        Flux<DeviceMessage> decoder = monitor.decode(
            connection,
            session,
            origin,
            Flux.error(error)
        );
        StepVerifier
            .create(monitor.beforeSendToPlatform(connection, session, origin, decoder))
            .expectErrorMatches(actual -> actual == error)
            .verify();

        StepVerifier
            .create(monitor.downstream(connection, session, origin, Mono.error(error)))
            .expectErrorMatches(actual -> actual == error)
            .verify();
    }

    private static class RecordingMonitor implements DeviceGatewayMonitor {

        private final String id;
        private final boolean allowConnection;
        private final boolean allowDecode;
        private final List<String> signals;

        private RecordingMonitor(String id,
                                 boolean allowConnection,
                                 boolean allowDecode,
                                 List<String> signals) {
            this.id = id;
            this.allowConnection = allowConnection;
            this.allowDecode = allowDecode;
            this.signals = signals;
        }

        @Override
        public boolean connected(ClientConnection connection) {
            signals.add(id + ":connected");
            return allowConnection;
        }

        @Override
        public void disconnected(ClientConnection connection) {
            signals.add(id + ":disconnected");
        }

        @Override
        public void rejected(ClientConnection connection, Throwable error) {
            signals.add(id + ":rejected");
        }

        @Override
        public boolean beforeDecode(ClientConnection connection, EncodedMessage message) {
            signals.add(id + ":beforeDecode");
            return allowDecode;
        }

        @Override
        public Flux<DeviceMessage> decode(ClientConnection connection,
                                          DeviceSession session,
                                          EncodedMessage origin,
                                          Flux<DeviceMessage> decoder) {
            return decoder.doOnNext(ignore -> signals.add(id + ":decode"));
        }

        @Override
        public Flux<DeviceMessage> beforeSendToPlatform(ClientConnection connection,
                                                        DeviceSession session,
                                                        EncodedMessage origin,
                                                        Flux<DeviceMessage> handler) {
            return handler.doOnNext(ignore -> signals.add(id + ":beforeSend"));
        }

        @Override
        public Mono<Void> downstream(ClientConnection connection,
                                     DeviceSession session,
                                     EncodedMessage origin,
                                     Mono<Void> sender) {
            return sender.doOnSuccess(ignore -> signals.add(id + ":downstream"));
        }
    }
}
