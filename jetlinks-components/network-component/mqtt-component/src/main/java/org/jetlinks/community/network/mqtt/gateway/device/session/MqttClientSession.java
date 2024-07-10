package org.jetlinks.community.network.mqtt.gateway.device.session;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * MQTT Client设备会话
 *
 * @author zhouhao
 * @since 1.0
 */
public class MqttClientSession implements PersistentSession {
    @Getter
    private final String id;

    @Getter
    private final DeviceOperator operator;

    private MqttClient clientTemp;

    @Getter
    @Setter
    private Mono<MqttClient> client;

    @Setter(AccessLevel.PROTECTED)
    private long connectTime = System.currentTimeMillis();

    @Setter(AccessLevel.PROTECTED)
    private long lastPingTime = System.currentTimeMillis();

    private long keepAliveTimeout = -1;

    private final DeviceGatewayMonitor monitor;

    @Getter
    @Setter
    private String gatewayId;

    public MqttClientSession(String id,
                             DeviceOperator operator,
                             MqttClient client,
                             DeviceGatewayMonitor monitor) {
        this(id, operator, Mono.just(client), monitor);
        this.clientTemp = client;
    }

    public MqttClientSession(String id,
                             DeviceOperator operator,
                             Mono<MqttClient> client,
                             DeviceGatewayMonitor monitor) {
        this.id = id;
        this.operator = operator;
        this.client = client;
        this.monitor = monitor;
    }

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
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
        if (encodedMessage instanceof MqttMessage) {
            if (monitor != null) {
                monitor.sentMessage();
            }
            return client
                .flatMap(client -> {
                    this.clientTemp = client;
                    return client
                        .publish(((MqttMessage) encodedMessage))
                        .thenReturn(true);
                });
        }
        return Mono.error(new DeviceOperationException
            .NoStackTrace(ErrorCode.UNSUPPORTED_MESSAGE, "error.unsupported_mqtt_message_type"));
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public void close() {

    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return (clientTemp == null || clientTemp.isAlive()) &&
            (keepAliveTimeout <= 0 || System.currentTimeMillis() - lastPingTime < keepAliveTimeout);
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        return client
            .map(client -> {
                this.clientTemp = client;
                return isAlive();
            });
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        this.keepAliveTimeout = timeout.toMillis();
    }

    @Override
    public String toString() {
        return "MqttClientSession{" +
            "id=" + id + ",device=" + getDeviceId() +
            '}';
    }

    @Override
    public String getProvider() {
        return MqttClientSessionPersistentProvider.PROVIDER;
    }
}
