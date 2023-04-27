package org.jetlinks.community.network.mqtt.gateway.device.session;

import lombok.Getter;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class UnknownDeviceMqttClientSession implements DeviceSession {
    @Getter
    private final String id;

    private final MqttClient client;

    private final DeviceGatewayMonitor monitor;
    private Duration keepAliveTimeout;

    public UnknownDeviceMqttClientSession(String id,
                                          MqttClient client,
                                          DeviceGatewayMonitor monitor) {
        this.id = id;
        this.client = client;
        this.monitor=monitor;
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
    public void setKeepAliveTimeout(Duration keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    @Override
    public Duration getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        if (encodedMessage instanceof MqttMessage) {
            return client
                .publish(((MqttMessage) encodedMessage))
                .doOnSuccess(ignore->monitor.sentMessage())
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
