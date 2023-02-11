package org.jetlinks.community.network.mqtt.gateway.device.session;

import lombok.Generated;
import lombok.Getter;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.ReplaceableDeviceSession;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * MQTT连接连接会话
 *
 * @author zhouhao
 * @since 1.0
 */
public class MqttConnectionSession implements DeviceSession, ReplaceableDeviceSession {

    @Getter
    @Generated
    private final String id;

    @Getter
    @Generated
    private final DeviceOperator operator;

    @Getter
    @Generated
    private final Transport transport;

    @Getter
    @Generated
    private MqttConnection connection;

    private final DeviceGatewayMonitor monitor;

    private final long connectTime = System.currentTimeMillis();

    public MqttConnectionSession(String id,
                                 DeviceOperator operator,
                                 Transport transport,
                                 MqttConnection connection,
                                 DeviceGatewayMonitor monitor) {
        this.id = id;
        this.operator = operator;
        this.transport = transport;
        this.connection = connection;
        this.monitor = monitor;
    }


    @Override
    public String getDeviceId() {
        return id;
    }

    @Override
    public long lastPingTime() {
        return connection.getLastPingTime();
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.defer(() -> connection.publish(((MqttMessage) encodedMessage)))
                   .doOnSuccess(nil -> monitor.sentMessage())
                   .thenReturn(true);
    }

    @Override
    public void close() {
        connection.close().subscribe();
    }

    @Override
    public void ping() {
        connection.keepAlive();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        connection.setKeepAliveTimeout(timeout);
    }

    @Override
    public boolean isAlive() {
        return connection.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        connection.onClose(c -> call.run());
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.ofNullable(connection.getClientAddress());
    }

    @Override
    public void replaceWith(DeviceSession session) {
        if (session instanceof MqttConnectionSession) {
            MqttConnectionSession connectionSession = ((MqttConnectionSession) session);
            if (!this.connection.equals(connectionSession.connection)) {
                this.connection.close().subscribe();
            }
            this.connection = connectionSession.connection;
        }
    }

    @Override
    public boolean isChanged(DeviceSession another) {
        if (another.isWrapFrom(MqttConnectionSession.class)) {
            return !this
                .connection
                .equals(another.unwrap(MqttConnectionSession.class).getConnection());
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqttConnectionSession that = (MqttConnectionSession) o;
        return Objects.equals(connection, that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection);
    }
}
