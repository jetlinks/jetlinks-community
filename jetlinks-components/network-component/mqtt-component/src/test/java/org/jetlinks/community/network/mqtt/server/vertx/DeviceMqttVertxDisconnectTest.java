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

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DevicePrincipal;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.mqtt.gateway.device.DeviceMqttConnection;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeviceMqttVertxDisconnectTest {

    @Test
    void disconnectDuringAuthenticationAndReconnectWithSameClientId() throws Exception {
        int aborted = 24;
        Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(2));
        MqttServer server = MqttServer.create(vertx, new MqttServerOptions()
            .setHost("127.0.0.1")
            .setPort(0));
        List<Socket> sockets = new ArrayList<>();
        BlockingQueue<MonoSink<DevicePrincipal>> pending = new LinkedBlockingQueue<>();
        CountDownLatch authStarted = new CountDownLatch(aborted);
        CountDownLatch abortedClosed = new CountDownLatch(aborted);
        CountDownLatch reconnectClosed = new CountDownLatch(1);
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger disconnections = new AtomicInteger();
        DeviceGatewayHelper helper = mock(DeviceGatewayHelper.class);
        DeviceGatewayMonitor monitor = mock(DeviceGatewayMonitor.class);
        DeviceRegistry registry = mock(DeviceRegistry.class);
        DeviceSessionManager manager = mock(DeviceSessionManager.class);
        when(helper.getRegistry()).thenReturn(registry);
        when(helper.getSessionManager()).thenReturn(manager);
        when(registry.resolveDevice(any())).thenReturn(Mono.<DevicePrincipal>create(sink -> {
            sink.onCancel(cancellations::incrementAndGet);
            pending.add(sink);
            authStarted.countDown();
        }));
        doAnswer(invocation -> {
            if (disconnections.incrementAndGet() <= aborted) {
                abortedClosed.countDown();
            } else {
                reconnectClosed.countDown();
            }
            return null;
        }).when(monitor).disconnected(any());
        server.endpointHandler(endpoint -> new DeviceMqttConnection(
            helper, monitor, new VertxMqttConnection(endpoint))
            .subscribe(new BaseSubscriber<>() {
                @Override
                protected void hookOnComplete() {
                    completions.incrementAndGet();
                }
            }));

        try {
            server.listen().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            byte[] packet = connectPacket("device-1");
            for (int i = 0; i < aborted; i++) {
                sockets.add(connect(server.actualPort(), packet));
            }
            assertTrue(authStarted.await(10, TimeUnit.SECONDS), "all CONNECT packets must enter authentication");
            List<MonoSink<DevicePrincipal>> abortedAuth = new ArrayList<>();
            pending.drainTo(abortedAuth);
            assertEquals(aborted, abortedAuth.size());

            for (Socket socket : sockets) {
                socket.close();
            }
            assertTrue(abortedClosed.await(10, TimeUnit.SECONDS), "socket close callbacks must complete");
            assertEquals(aborted, cancellations.get());
            assertEquals(aborted, completions.get());
            Socket reconnect = connect(server.actualPort(), packet);
            sockets.add(reconnect);
            MonoSink<DevicePrincipal> currentAuth = pending.poll(5, TimeUnit.SECONDS);
            assertNotNull(currentAuth);
            DevicePrincipal principal = mock(DevicePrincipal.class);
            DeviceOperator operator = mock(DeviceOperator.class);
            when(principal.isVerified()).thenReturn(true);
            when(principal.getDevice()).thenReturn(operator);
            when(operator.getDeviceId()).thenReturn("device-1");
            abortedAuth.forEach(sink -> sink.success(principal));
            verifyNoInteractions(manager);

            DeviceSession session = mock(DeviceSession.class);
            when(manager.compute(eq("device-1"), any(Mono.class), any(Function.class)))
                .thenReturn(Mono.just(session));
            currentAuth.success(principal);
            reconnect.setSoTimeout(5000);
            byte[] connack = reconnect.getInputStream().readNBytes(4);
            assertEquals(4, connack.length);
            assertEquals(0x20, connack[0] & 0xff);
            assertEquals(0, connack[3]);
            reconnect.close();
            assertTrue(reconnectClosed.await(5, TimeUnit.SECONDS));
            assertEquals(aborted + 1, completions.get());
        } finally {
            for (Socket socket : sockets) {
                socket.close();
            }
            server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    private static Socket connect(int port, byte[] packet) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
        socket.getOutputStream().write(packet);
        socket.getOutputStream().flush();
        return socket;
    }

    private static byte[] connectPacket(String clientId) {
        EmbeddedChannel encoder = new EmbeddedChannel(MqttEncoder.INSTANCE);
        try {
            encoder.writeOutbound(MqttMessageBuilders.connect()
                .clientId(clientId)
                .cleanSession(true)
                .build());
            ByteBuf encoded = encoder.readOutbound();
            try {
                byte[] packet = new byte[encoded.readableBytes()];
                encoded.readBytes(packet);
                return packet;
            } finally {
                encoded.release();
            }
        } finally {
            encoder.finishAndReleaseAll();
        }
    }
}
