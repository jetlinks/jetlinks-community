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
package org.jetlinks.community.network.mqtt.server.vertx;

import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VertxMqttServerDispatchTest {

    @Test
    void shouldRejectNormallyWhenLastSubscriberCancelsAfterEmptyCheck() {
        verifyCancellation(true, false);
    }

    @Test
    void shouldRejectNormallyWhenLastSubscriberCancelsAfterSizeRead() {
        verifyCancellation(false, false);
    }

    @Test
    void shouldContinueDispatchingToAnotherHolderAfterCancellation() {
        verifyCancellation(true, true);
    }

    @Test
    void shouldDeliverToActiveHolder() {
        VertxMqttConnection connection = mock(VertxMqttConnection.class);
        VertxMqttServer server = server(connection);
        AtomicReference<MqttConnection> delivered = new AtomicReference<>();
        Disposable subscription = server.handleConnection("active").subscribe(delivered::set);
        try {
            dispatch(server, connection);
            assertSame(connection, delivered.get());
            verify(connection, never()).reject(any());
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void shouldPublishImmutableSnapshotsAndRemoveOnlyCancelledSubscription() {
        VertxMqttConnection connection = mock(VertxMqttConnection.class);
        VertxMqttServer server = server(connection);
        Map<String, List<Sinks.Many<MqttConnection>>> holders = holders(server);
        Disposable first = server.handleConnection("shared").subscribe();
        List<Sinks.Many<MqttConnection>> before = holders.get("shared");
        AtomicReference<MqttConnection> delivered = new AtomicReference<>();
        Disposable second = server.handleConnection("shared").subscribe(delivered::set);
        try {
            List<Sinks.Many<MqttConnection>> both = holders.get("shared");
            assertNotSame(before, both);
            assertEquals(1, before.size());
            assertEquals(2, both.size());
            assertThrows(UnsupportedOperationException.class, before::clear);
            first.dispose();
            assertEquals(1, holders.get("shared").size());
            assertEquals(2, both.size());
            dispatch(server, connection);
            assertSame(connection, delivered.get());
        } finally {
            first.dispose();
            second.dispose();
        }
        assertFalse(holders.containsKey("shared"));
    }

    @Test
    void shouldDispatchWithoutCopyingSubscriptionSnapshot() {
        VertxMqttConnection connection = mock(VertxMqttConnection.class);
        VertxMqttServer server = server(connection);
        AtomicReference<MqttConnection> delivered = new AtomicReference<>();
        Disposable subscription = server.handleConnection("active").subscribe(delivered::set);
        Map<String, List<Sinks.Many<MqttConnection>>> holders = holders(server);
        List<Sinks.Many<MqttConnection>> snapshot = holders.get("active");
        AtomicInteger copies = new AtomicInteger();
        holders.put("active", new AbstractList<Sinks.Many<MqttConnection>>() {
            @Override
            public Sinks.Many<MqttConnection> get(int index) {
                return snapshot.get(index);
            }

            @Override
            public int size() {
                return snapshot.size();
            }

            @Override
            public Object[] toArray() {
                copies.incrementAndGet();
                return snapshot.toArray();
            }
        });
        try {
            dispatch(server, connection);
            assertSame(connection, delivered.get());
            assertEquals(0, copies.get());
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void shouldRetainConcurrentRegistrationsAndCancellations() throws Exception {
        VertxMqttConnection connection = mock(VertxMqttConnection.class);
        VertxMqttServer server = server(connection);
        AtomicInteger delivered = new AtomicInteger();
        List<Callable<Disposable>> operations = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            operations.add(() -> server.handleConnection("shared").subscribe(ignore -> delivered.incrementAndGet()));
        }
        List<Disposable> subscriptions = concurrently(operations);
        try {
            assertEquals(32, holders(server).get("shared").size());
            operations.clear();
            for (int i = 0; i < 16; i++) {
                Disposable cancelled = subscriptions.get(i);
                operations.add(() -> {
                    cancelled.dispose();
                    return null;
                });
                operations.add(() -> server.handleConnection("shared").subscribe(ignore -> delivered.incrementAndGet()));
            }
            subscriptions.addAll(concurrently(operations));
            assertEquals(32, holders(server).get("shared").size());
            dispatch(server, connection);
            assertEquals(1, delivered.get());
            verify(connection, never()).reject(any());
        } finally {
            for (Disposable subscription : subscriptions) {
                if (subscription != null) {
                    subscription.dispose();
                }
            }
        }
        assertFalse(holders(server).containsKey("shared"));
    }

    private void verifyCancellation(boolean afterEmptyCheck, boolean anotherHolder) {
        VertxMqttConnection connection = mock(VertxMqttConnection.class);
        VertxMqttServer server = server(connection);
        Map<String, List<Sinks.Many<MqttConnection>>> holders = holders(server);
        Disposable cancelled = server.handleConnection("cancelled").subscribe();
        CancellingSnapshot<Sinks.Many<MqttConnection>> snapshot =
            new CancellingSnapshot<>(holders.get("cancelled"), afterEmptyCheck);
        holders.put("cancelled", snapshot);
        AtomicReference<MqttConnection> delivered = new AtomicReference<>();
        Disposable active = anotherHolder ? server.handleConnection("active").subscribe(delivered::set) : null;
        snapshot.cancellation = cancelled::dispose;
        try {
            assertDoesNotThrow(() -> dispatch(server, connection));
            assertFalse(holders.containsKey("cancelled"));
            assertEquals(1, snapshot.size());
            if (anotherHolder) {
                assertSame(connection, delivered.get());
                verify(connection, never()).reject(any());
            } else {
                verify(connection).reject(any());
            }
        } finally {
            cancelled.dispose();
            if (active != null) {
                active.dispose();
            }
        }
    }

    private <T> List<T> concurrently(List<Callable<T>> operations) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> operation : operations) {
                futures.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return operation.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Sinks.Many<MqttConnection>>> holders(VertxMqttServer server) {
        Map<String, List<Sinks.Many<MqttConnection>>> holders =
            (Map<String, List<Sinks.Many<MqttConnection>>>) ReflectionTestUtils.getField(server, "sinks");
        assertNotNull(holders);
        return holders;
    }

    private VertxMqttServer server(VertxMqttConnection connection) {
        return new VertxMqttServer("test");
    }

    private void dispatch(VertxMqttServer server, VertxMqttConnection connection) {
        ReflectionTestUtils.invokeMethod(server, "handleConnection", connection);
    }

    // 捕获后发布的新快照不会改变当前快照；在判空、读取边界时触发真实取消。
    private static class CancellingSnapshot<E> extends AbstractList<E> {
        private final List<E> snapshot;
        private final boolean afterEmptyCheck;
        private Runnable cancellation;

        private CancellingSnapshot(List<E> snapshot, boolean afterEmptyCheck) {
            this.snapshot = snapshot;
            this.afterEmptyCheck = afterEmptyCheck;
        }

        @Override
        public E get(int index) {
            return snapshot.get(index);
        }

        @Override
        public boolean isEmpty() {
            boolean empty = snapshot.isEmpty();
            if (afterEmptyCheck && !empty) {
                cancel();
            }
            return empty;
        }

        @Override
        public int size() {
            int size = snapshot.size();
            if (!afterEmptyCheck && size > 0) {
                cancel();
            }
            return size;
        }

        private void cancel() {
            Runnable action = cancellation;
            cancellation = null;
            if (action != null) {
                action.run();
            }
        }
    }
}
