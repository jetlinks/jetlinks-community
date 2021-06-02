package org.jetlinks.community.network.mqtt.gateway.device.session;

import lombok.Getter;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

public class UnknownDeviceMqttClientSession implements DeviceSession {
    @Getter
    private String id;

    private MqttClient client;

    public UnknownDeviceMqttClientSession(String id,
                                          MqttClient client) {
        this.id = id;
        this.client = client;
    }

    @Override
    public String getDeviceId() {
        return null;
    }

    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public long lastPingTime() {
        return 0;
    }

    @Override
    public long connectTime() {
        return 0;
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

    }

    @Override
    public boolean isAlive() {
        return client.isAlive();
    }

    @Override
    public void onClose(Runnable call) {

    }
}
