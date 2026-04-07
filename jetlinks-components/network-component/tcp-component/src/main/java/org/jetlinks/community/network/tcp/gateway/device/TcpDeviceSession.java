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

import lombok.Getter;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.core.server.session.MultiConnectionDeviceSession;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

class TcpDeviceSession extends MultiConnectionDeviceSession<TcpClient> {

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    private final DeviceGatewayMonitor monitor;

    TcpDeviceSession(DeviceOperator operator,
                     Transport transport,
                     DeviceGatewayMonitor monitor,
                     DeviceSessionManager sessionManager) {
        super(operator.getId(), operator, sessionManager);
        this.transport = transport;
        this.monitor = monitor;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.defer(() -> {
            monitor.sentMessage();
            TcpClient client = takeConnection();
            if (client == null) {
                return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
            }
            return client.send(new TcpMessage(encodedMessage.getPayload()));
        });
    }


    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
        for (TcpClient client : this) {
            client.keepAlive();
        }
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        for (TcpClient client : this) {
            client.setKeepAliveTimeout(timeout);
        }
    }

    @Override
    public boolean isChanged(DeviceSession another) {
        if (another.isWrapFrom(TcpDeviceSession.class)) {
            return !Objects.equals(another.getId(), getId());
        }
        return true;
    }

}
