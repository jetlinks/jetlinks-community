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
package org.jetlinks.community.network.mqtt.gateway.device.session;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.util.internal.ThreadLocalRandom;
import jakarta.annotation.Nonnull;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.ClientConnectionSession;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.ReplaceableDeviceSession;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * MQTT连接连接会话
 *
 * @author zhouhao
 * @since 1.0
 */
public class MqttConnectionSession extends CopyOnWriteArrayList<MqttConnection>
    implements DeviceSession, ReplaceableDeviceSession, Consumer<MqttConnection>, Scannable, ClientConnectionSession {

    private static final int maxConnections = Integer.getInteger(
        "mqtt.device-session.max-connection", 64
    );

    private final Disposable.Composite disposable = Disposables.composite();

    private final DeviceSessionManager sessionManager;

    @Getter
    @Generated
    private final String id;

    @Getter
    @Generated
    private final DeviceOperator operator;

    @Getter
    @Generated
    private final Transport transport;

    private final DeviceGatewayMonitor monitor;

    private long connectTime = System.currentTimeMillis();

    public MqttConnectionSession(String id,
                                 DeviceOperator operator,
                                 Transport transport,
                                 MqttConnection connection,
                                 DeviceGatewayMonitor monitor,
                                 DeviceSessionManager sessionManager) {
        this.id = id;
        this.operator = operator;
        this.transport = transport;
        this.monitor = monitor;
        this.sessionManager = sessionManager;
        registerConnection(connection);
    }

    public void registerConnection(MqttConnection connection) {
        if (disposable.isDisposed() || size() >= maxConnections) {
            connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
            return;
        }
        //不能在此以 isAlive() 做准入判断:注册发生在 CONNACK(accept)之前,
        //该守卫会把握手中的新连接静默丢弃,造成上行正常而全部下行报"设备连接已断开"。
        //握手期连接的存活语义由 VertxMqttConnection.isAlive()(closed 标志)保证,
        //失活连接由 takeConnection/onClose 兜底清理。
        this.add(connection);
        connectTime = System.currentTimeMillis();
        connection.onClose(this);
    }

    public void unregisterConnection(MqttConnection connection) {
        this.remove(connection);
    }

    @Override
    public String getDeviceId() {
        return id;
    }

    // disconnect
    @Override
    public void accept(MqttConnection mqttConnection) {
        unregisterConnection(mqttConnection);
        //check session
        sessionManager
            .getSession(getDeviceId(), true)
            .subscribe();
    }

    @Override
    public long lastPingTime() {
        return this
            .stream()
            .mapToLong(MqttConnection::getLastPingTime)
            .max()
            .orElse(0);
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    private MqttConnection takeConnection() {
        MqttConnection connection;
        do {
            synchronized (this) {
                int size = this.size();
                if (size == 0) {
                    return null;
                }
                if (size == 1) {
                    connection = this.get(0);
                } else {
                    connection = this.get(ThreadLocalRandom.current().nextInt(size));
                }
            }
            if (connection.isAlive()) {
                return connection;
            }
            connection.disconnect();
            accept(connection);
        } while (true);

    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        MqttConnection connection = takeConnection();
        if (connection == null) {
            return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
        }
        return Mono
            .defer(() -> connection.publish(((MqttMessage) encodedMessage)))
            .doOnSuccess(nil -> monitor.sentMessage())
            .thenReturn(true);
    }

    @Override
    public void close() {
        disposable.dispose();
        synchronized (this) {
            for (MqttConnection conn : this) {
                conn.disconnect();
            }
            clear();
        }
    }

    @Override
    public void ping() {
        for (MqttConnection conn : this) {
            conn.keepAlive();
        }
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        for (MqttConnection conn : this) {
            conn.setKeepAliveTimeout(timeout);
        }
    }

    @Override
    public boolean isAlive() {
        if (disposable.isDisposed()) {
            return false;
        }
        boolean alive = false;
        for (MqttConnection conn : this) {
            alive |= conn.isAlive();
        }
        return alive;
    }

    @Override
    public void onClose(Runnable call) {
        disposable.add(call::run);
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional
            .ofNullable(takeConnection())
            .map(MqttConnection::getClientAddress);

    }

    @Override
    public void replaceWith(DeviceSession session) {
        if (session instanceof MqttConnectionSession) {
            MqttConnectionSession connectionSession = ((MqttConnectionSession) session);
            synchronized (this) {
                if (!this.equals(connectionSession)) {
                    for (MqttConnection connection : this) {
                        connection.close().subscribe();
                    }
                    this.clear();
                }
                this.addAll(connectionSession);
            }
        }
    }

    @Override
    public boolean isChanged(DeviceSession another) {
        if (another.isWrapFrom(MqttConnectionSession.class)) {
            return !this.equals(another.unwrap(MqttConnectionSession.class));
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr key) {
        // 只支持获取buffer
        if (key == Attr.BUFFERED) {
            return this
                .stream()
                .mapToInt(conn -> conn.scanOrDefault(Attr.BUFFERED, 0))
                .sum();
        }

        if (key == Attr.LARGE_BUFFERED) {
            return this
                .stream()
                .mapToLong(conn -> conn.scanOrDefault(Attr.BUFFERED, 0))
                .sum();
        }


        return null;
    }

    @Override
    public Collection<? extends ClientConnection> getConnections() {
        return Collections.unmodifiableCollection(this);
    }
}
