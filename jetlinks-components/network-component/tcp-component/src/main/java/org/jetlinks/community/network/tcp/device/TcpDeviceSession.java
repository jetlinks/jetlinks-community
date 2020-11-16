package org.jetlinks.community.network.tcp.device;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

class TcpDeviceSession implements DeviceSession {

    @Getter
    private String id;

    @Getter
    @Setter
    private DeviceOperator operator;

    @Setter
    private TcpClient client;

    @Getter
    private Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private long connectTime = System.currentTimeMillis();

    TcpDeviceSession(String id, DeviceOperator operator, TcpClient client, Transport transport) {
        this.id = id;
        this.operator = operator;
        this.client = client;
        this.transport = transport;
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
        return client.send(new TcpMessage(encodedMessage.getPayload()));
    }

    @Override
    public void close() {
        client.shutdown();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
        client.keepAlive();
    }


    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        client.setKeepAliveTimeout(timeout);
    }

    @Override
    public boolean isAlive() {
        return client.isAlive();
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.ofNullable(client.getRemoteAddress());
    }

    @Override
    public void onClose(Runnable call) {
        client.onDisconnect(call);
    }
}
