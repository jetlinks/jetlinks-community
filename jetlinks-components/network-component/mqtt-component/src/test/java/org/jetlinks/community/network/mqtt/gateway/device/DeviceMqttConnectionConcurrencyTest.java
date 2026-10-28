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
package org.jetlinks.community.network.mqtt.gateway.device;

import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DevicePrincipal;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.server.mqtt.MqttAuth;
import org.jetlinks.core.server.session.DeviceSession;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceMqttConnectionConcurrencyTest {

    @Test
    void synchronousAuthAndSocketCloseMustNotInvertLocks() throws InterruptedException {
        Object socketLock = new Object();
        CountDownLatch socketLocked = new CountDownLatch(1);
        CountDownLatch readingSocket = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(2);
        AtomicReference<Consumer<MqttConnection>> closeListener = new AtomicReference<>();
        AtomicInteger completions = new AtomicInteger();
        MqttConnection client = mock(MqttConnection.class);
        DeviceGatewayMonitor monitor = mock(DeviceGatewayMonitor.class);
        when(client.getClientId()).thenReturn("device-1");
        when(client.close()).thenReturn(Mono.empty());
        doAnswer(invocation -> {
            closeListener.set(invocation.getArgument(0));
            return null;
        }).when(client).onClose(any());

        DeviceMqttConnection device = new DeviceMqttConnection(mock(DeviceGatewayHelper.class), monitor, client) {
            @Override
            void doAuth() {
                // Model the synchronous tracer address lookup while a close callback owns the socket lock.
                readingSocket.countDown();
                synchronized (socketLock) {
                    client.getClientAddress();
                }
            }
        };

        Thread closing = new Thread(() -> {
            synchronized (socketLock) {
                socketLocked.countDown();
                try {
                    if (!readingSocket.await(3, TimeUnit.SECONDS)) {
                        return;
                    }
                    closeListener.get().accept(client);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            }
        }, "mqtt-close-under-socket-lock");
        closing.setDaemon(true);
        closing.start();
        assertTrue(socketLocked.await(3, TimeUnit.SECONDS));

        Thread subscribing = new Thread(() -> {
            try {
                device.subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnComplete() {
                        completions.incrementAndGet();
                    }
                });
            } finally {
                finished.countDown();
            }
        }, "mqtt-synchronous-auth");
        subscribing.setDaemon(true);
        subscribing.start();

        assertTrue(finished.await(3, TimeUnit.SECONDS), "socket close and synchronous authentication must finish");
        assertEquals(1, completions.get());
        verify(monitor).disconnected(client);
    }

    @Test
    void cancelFromOnSubscribeMustCompleteAfterSubscription() {
        MqttConnection client = mock(MqttConnection.class);
        DeviceGatewayMonitor monitor = mock(DeviceGatewayMonitor.class);
        when(client.close()).thenReturn(Mono.empty());
        AtomicInteger authCalls = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        DeviceMqttConnection device = new DeviceMqttConnection(mock(DeviceGatewayHelper.class), monitor, client) {
            @Override
            void doAuth() {
                authCalls.incrementAndGet();
            }
        };

        device.subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                subscription.cancel();
            }

            @Override
            protected void hookOnComplete() {
                completions.incrementAndGet();
            }
        });
        device.cancel();

        assertEquals(0, authCalls.get());
        assertEquals(1, completions.get());
        verify(client).close();
        verify(monitor).disconnected(client);
    }

    @Test
    void closeCancelsPendingSessionRegistrationBeforeItCanAccept() {
        MqttConnection client = mock(MqttConnection.class);
        when(client.close()).thenReturn(Mono.empty());
        DeviceGatewayHelper helper = mock(DeviceGatewayHelper.class);
        DeviceSessionManager manager = mock(DeviceSessionManager.class);
        DeviceGatewayMonitor monitor = mock(DeviceGatewayMonitor.class);
        DeviceOperator operator = mock(DeviceOperator.class);
        when(helper.getSessionManager()).thenReturn(manager);
        when(operator.getDeviceId()).thenReturn("device-1");
        AtomicReference<MonoSink<DeviceSession>> registration = new AtomicReference<>();
        AtomicInteger cancellations = new AtomicInteger();
        when(manager.compute(eq("device-1"), any(Mono.class), any(Function.class)))
            .thenReturn(Mono.<DeviceSession>create(sink -> {
                registration.set(sink);
                sink.onCancel(cancellations::incrementAndGet);
            }));
        DeviceMqttConnection device = new DeviceMqttConnection(helper, monitor, client) {
            @Override
            void doAuth() {
            }
        };

        device.subscribe(new BaseSubscriber<>() {});
        device.handleAuth(operator, AuthenticationResponse.success());
        assertNotNull(registration.get());
        device.cancel();
        registration.get().success(mock(DeviceSession.class));

        assertEquals(1, cancellations.get());
        verify(client, never()).accept();
    }

    @Test
    void closeCancelsPendingAuthenticationBeforeItCanAccept() {
        MqttConnection client = mock(MqttConnection.class);
        DeviceGatewayHelper helper = mock(DeviceGatewayHelper.class);
        DeviceRegistry registry = mock(DeviceRegistry.class);
        DeviceGatewayMonitor monitor = mock(DeviceGatewayMonitor.class);
        when(client.getClientId()).thenReturn("device-1");
        when(client.getAuth()).thenReturn(Optional.of(mock(MqttAuth.class)));
        when(client.close()).thenReturn(Mono.empty());
        when(helper.getRegistry()).thenReturn(registry);
        AtomicReference<MonoSink<DevicePrincipal>> pending = new AtomicReference<>();
        AtomicInteger cancellations = new AtomicInteger();
        when(registry.resolveDevice(any()))
            .thenReturn(Mono.<DevicePrincipal>create(sink -> {
                pending.set(sink);
                sink.onCancel(cancellations::incrementAndGet);
            }));

        DeviceMqttConnection device = new DeviceMqttConnection(helper, monitor, client);
        device.subscribe(new BaseSubscriber<>() {});
        assertNotNull(pending.get());
        device.cancel();
        pending.get().success(mock(DevicePrincipal.class));

        assertEquals(1, cancellations.get());
        verify(client, never()).accept();
        verify(monitor).disconnected(client);
    }

    @Test
    void completedAuthenticationAndRegistrationAreNotRetained() {
        MqttConnection client = mock(MqttConnection.class);
        DeviceGatewayHelper helper = mock(DeviceGatewayHelper.class);
        DeviceRegistry registry = mock(DeviceRegistry.class);
        DeviceSessionManager manager = mock(DeviceSessionManager.class);
        DeviceGatewayMonitor monitor = mock(DeviceGatewayMonitor.class);
        DevicePrincipal principal = mock(DevicePrincipal.class);
        DeviceOperator operator = mock(DeviceOperator.class);
        DeviceSession session = mock(DeviceSession.class);
        when(client.getClientId()).thenReturn("device-1");
        when(client.getAuth()).thenReturn(Optional.of(mock(MqttAuth.class)));
        when(client.handleMessage()).thenReturn(Flux.never());
        when(client.close()).thenReturn(Mono.empty());
        when(helper.getRegistry()).thenReturn(registry);
        when(helper.getSessionManager()).thenReturn(manager);
        when(registry.resolveDevice(any())).thenReturn(Mono.just(principal));
        when(principal.isVerified()).thenReturn(true);
        when(principal.getDevice()).thenReturn(operator);
        when(operator.getDeviceId()).thenReturn("device-1");
        when(manager.compute(eq("device-1"), any(Mono.class), any(Function.class)))
            .thenReturn(Mono.just(session));

        DeviceMqttConnection device = new DeviceMqttConnection(helper, monitor, client);
        device.subscribe(new BaseSubscriber<>() {});

        Disposable.Composite tasks = (Disposable.Composite) ReflectionTestUtils.getField(device, "disposable");
        assertNotNull(tasks);
        assertEquals(1, tasks.size(), "only the active message subscription should remain");
        device.cancel();
    }

    @Test
    void concurrentReconnectAndCloseMustTerminateEachConnectionOnce() throws Exception {
        int connections = 128;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger disconnections = new AtomicInteger();
        DeviceGatewayMonitor monitor = mock(DeviceGatewayMonitor.class);
        doAnswer(invocation -> {
            disconnections.incrementAndGet();
            return null;
        }).when(monitor).disconnected(any());
        List<MqttConnection> clients = new ArrayList<>();
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < connections; i++) {
                MqttConnection client = mock(MqttConnection.class);
                when(client.getClientId()).thenReturn("reconnecting-device");
                when(client.close()).thenReturn(Mono.empty());
                DeviceMqttConnection device = new DeviceMqttConnection(mock(DeviceGatewayHelper.class), monitor, client) {
                    @Override
                    void doAuth() {
                    }
                };
                clients.add(client);
                tasks.add(executor.submit(() -> {
                    start.await();
                    device.subscribe(new BaseSubscriber<>() {
                        @Override
                        protected void hookOnComplete() {
                            completions.incrementAndGet();
                        }
                    });
                    return null;
                }));
                tasks.add(executor.submit(() -> {
                    start.await();
                    device.cancel();
                    device.cancel();
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        assertEquals(connections, completions.get());
        assertEquals(connections, disconnections.get());
        for (MqttConnection client : clients) {
            verify(client, times(1)).close();
        }
    }
}
