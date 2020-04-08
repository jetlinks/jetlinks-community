package org.jetlinks.community.network.mqtt.gateway.device.session;

import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class MqttClientSession implements DeviceSession {
    @Getter
    private String id;

    @Getter
    private DeviceOperator operator;

    private MqttClient client;

    private long connectTime = System.currentTimeMillis();

    private long lastPingTime = System.currentTimeMillis();

    private long keepAliveTimeout = -1;

    public MqttClientSession(String id,
                             DeviceOperator operator,
                             MqttClient client) {
        this.id = id;
        this.operator = operator;
        this.client = client;
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
            return client.publish(((MqttMessage) encodedMessage))
                .thenReturn(true);
        }
        return Mono.error(new UnsupportedOperationException("unsupported message type:" + encodedMessage.getClass()));
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
        return client.isAlive() &&
            (keepAliveTimeout <= 0 || System.currentTimeMillis() - lastPingTime < keepAliveTimeout);
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
}
