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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

class CompositeDeviceGatewayMonitor implements DeviceGatewayMonitor {

    private final List<DeviceGatewayMonitor> monitors = new ArrayList<>();

    public CompositeDeviceGatewayMonitor add(DeviceGatewayMonitor... monitors) {
        return add(Arrays.asList(monitors));
    }

    public CompositeDeviceGatewayMonitor add(Collection<DeviceGatewayMonitor> monitors) {
        this.monitors.addAll(monitors);
        return this;
    }

    protected void doWith(Consumer<DeviceGatewayMonitor> monitorConsumer) {
        monitors.forEach(monitorConsumer);
    }


    @Override
    public void totalConnection(long total) {
        doWith(monitor -> monitor.totalConnection(total));
    }

    @Override
    public void connected() {
        doWith(DeviceGatewayMonitor::connected);
    }

    @Override
    public void rejected() {
        doWith(DeviceGatewayMonitor::rejected);
    }

    @Override
    public void disconnected() {
        doWith(DeviceGatewayMonitor::disconnected);
    }

    @Override
    public void receivedMessage() {
        doWith(DeviceGatewayMonitor::receivedMessage);
    }

    @Override
    public void sentMessage() {
        doWith(DeviceGatewayMonitor::sentMessage);
    }

    @Override
    public boolean connected(ClientConnection connection) {
        boolean accepted = true;
        for (DeviceGatewayMonitor monitor : monitors) {
            if (!monitor.connected(connection)) {
                accepted = false;
            }
        }
        return accepted;
    }

    @Override
    public void disconnected(ClientConnection connection) {
        doWith(monitor -> monitor.disconnected(connection));
    }

    @Override
    public void rejected(ClientConnection connection, @Nullable Throwable error) {
        doWith(monitor -> monitor.rejected(connection, error));
    }

    @Override
    public boolean beforeDecode(@Nullable ClientConnection connection, EncodedMessage message) {
        boolean accepted = true;
        for (DeviceGatewayMonitor monitor : monitors) {
            if (!monitor.beforeDecode(connection, message)) {
                accepted = false;
            }
        }
        return accepted;
    }

    @Override
    public Flux<DeviceMessage> decode(@Nullable ClientConnection connection,
                                      DeviceSession session,
                                      EncodedMessage origin,
                                      Flux<DeviceMessage> decoder) {
        for (DeviceGatewayMonitor monitor : monitors) {
            decoder = monitor.decode(connection, session, origin, decoder);
        }
        return decoder;
    }

    @Override
    public Flux<DeviceMessage> handleUpstream(@Nullable ClientConnection connection,
                                              DeviceSession session,
                                              EncodedMessage origin,
                                              Flux<DeviceMessage> decoder,
                                              UnaryOperator<Flux<DeviceMessage>> platformHandler) {
        // 平台处理器作为链尾只组合一次，同时保留 beforeSendToPlatform 的注册顺序。
        UnaryOperator<Flux<DeviceMessage>> handler = platformHandler;
        for (DeviceGatewayMonitor monitor : monitors) {
            UnaryOperator<Flux<DeviceMessage>> next = handler;
            handler = source -> monitor.handleUpstream(connection, session, origin, source, next);
        }
        return handler.apply(decoder);
    }

    @Override
    public Flux<DeviceMessage> beforeSendToPlatform(@Nullable ClientConnection connection,
                                                    DeviceSession session,
                                                    EncodedMessage origin,
                                                    Flux<DeviceMessage> handler) {
        for (DeviceGatewayMonitor monitor : monitors) {
            handler = monitor.beforeSendToPlatform(connection, session, origin, handler);
        }
        return handler;
    }

    @Override
    public Mono<Void> downstream(ClientConnection connection,
                                 DeviceSession session,
                                 EncodedMessage origin,
                                 Mono<Void> sender) {
        for (DeviceGatewayMonitor monitor : monitors) {
            sender = monitor.downstream(connection, session, origin, sender);
        }
        return sender;
    }
}
