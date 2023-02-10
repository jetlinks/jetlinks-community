package org.jetlinks.community.network.http.device;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.http.server.HttpExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

class UnknownHttpDeviceSession implements DeviceSession {

    private final HttpExchange exchange;

    public UnknownHttpDeviceSession(HttpExchange exchange) {
        this.exchange = exchange;

    }

    private Duration timeout;

    @Override
    public String getId() {
        return "unknown";
    }

    @Override
    public String getDeviceId() {
        return "unknown";
    }

    @Nullable
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
        return Mono.empty();
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public void close() {

    }

    @Override
    public void ping() {

    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public Duration getKeepAliveTimeout() {
        return timeout;
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.of(exchange.request().getClientAddress());
    }
}
