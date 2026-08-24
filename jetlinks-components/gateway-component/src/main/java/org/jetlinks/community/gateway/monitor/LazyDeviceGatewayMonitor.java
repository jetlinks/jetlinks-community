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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

class LazyDeviceGatewayMonitor implements DeviceGatewayMonitor {

    private volatile DeviceGatewayMonitor target;

    private final Supplier<DeviceGatewayMonitor> monitorSupplier;

    public LazyDeviceGatewayMonitor(Supplier<DeviceGatewayMonitor> monitorSupplier) {
        this.monitorSupplier = monitorSupplier;
    }

    public DeviceGatewayMonitor getTarget() {
        if (target == null) {
            target = monitorSupplier.get();
        }
        return target;
    }


    @Override
    public void totalConnection(long total) {
        getTarget().totalConnection(total);
    }

    @Override
    public void connected() {
        getTarget().connected();
    }

    @Override
    public void rejected() {
        getTarget().rejected();
    }

    @Override
    public void disconnected() {
        getTarget().disconnected();
    }

    @Override
    public void receivedMessage() {
        getTarget().receivedMessage();
    }

    @Override
    public void sentMessage() {
        getTarget().sentMessage();
    }

    @Override
    public boolean connected(ClientConnection connection) {
        return getTarget().connected(connection);
    }

    @Override
    public void disconnected(ClientConnection connection) {
        getTarget().disconnected(connection);
    }

    @Override
    public void rejected(ClientConnection connection, @Nullable Throwable error) {
        getTarget().rejected(connection, error);
    }

    @Override
    public boolean beforeDecode(@Nullable ClientConnection connection, EncodedMessage message) {
        return getTarget().beforeDecode(connection, message);
    }

    @Override
    public Flux<DeviceMessage> decode(@Nullable ClientConnection connection,
                                      DeviceSession session,
                                      EncodedMessage origin,
                                      Flux<DeviceMessage> decoder) {
        return getTarget().decode(connection, session, origin, decoder);
    }

    @Override
    public Flux<DeviceMessage> handleUpstream(@Nullable ClientConnection connection,
                                              DeviceSession session,
                                              EncodedMessage origin,
                                              Flux<DeviceMessage> decoder,
                                              UnaryOperator<Flux<DeviceMessage>> platformHandler) {
        return getTarget().handleUpstream(connection, session, origin, decoder, platformHandler);
    }

    @Override
    public Flux<DeviceMessage> beforeSendToPlatform(@Nullable ClientConnection connection,
                                                    DeviceSession session,
                                                    EncodedMessage origin,
                                                    Flux<DeviceMessage> handler) {
        return getTarget().beforeSendToPlatform(connection, session, origin, handler);
    }

    @Override
    public Mono<Void> downstream(ClientConnection connection,
                                 DeviceSession session,
                                 EncodedMessage origin,
                                 Mono<Void> sender) {
        return getTarget().downstream(connection, session, origin, sender);
    }
}
