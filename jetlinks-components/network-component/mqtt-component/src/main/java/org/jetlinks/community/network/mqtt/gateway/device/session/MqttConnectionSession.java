package org.jetlinks.community.network.mqtt.gateway.device.session;

import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import reactor.core.publisher.Mono;

public class MqttConnectionSession implements DeviceSession {

    @Getter
    private String id;

    @Getter
    private DeviceOperator operator;

    @Getter
    private Transport transport;

    @Getter
    private MqttConnection connection;

    public MqttConnectionSession(String id,DeviceOperator operator,Transport transport,MqttConnection connection){
        this.id=id;
        this.operator=operator;
        this.transport=transport;
        this.connection=connection;
    }

    private long connectTime = System.currentTimeMillis();

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
            .thenReturn(true);
    }

    @Override
    public void close() {
        connection.close().subscribe();
    }

    @Override
    public void ping() {

    }

    @Override
    public boolean isAlive() {
        return connection.isAlive();
    }

    @Override
    public void onClose(Runnable call) {

    }
}
