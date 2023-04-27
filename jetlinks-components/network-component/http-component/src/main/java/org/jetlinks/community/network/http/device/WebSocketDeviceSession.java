package org.jetlinks.community.network.http.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.http.server.WebSocketExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * WebSocket设备会话
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
class WebSocketDeviceSession implements DeviceSession {

    @Getter
    @Setter
    private volatile DeviceOperator operator;

    @Setter
    private WebSocketExchange exchange;

    private final long connectTime = System.currentTimeMillis();

    private Duration keepAliveTimeout;

    public WebSocketDeviceSession(DeviceOperator device, WebSocketExchange exchange) {
        this.operator = device;
        this.exchange = exchange;
    }

    @Override
    public String getId() {
        return getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator == null ? "unknown" : operator.getDeviceId();
    }

    @Override
    public long lastPingTime() {
        return exchange.getLastKeepAliveTime();
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        if (encodedMessage instanceof WebSocketMessage) {
            return exchange
                .send(((WebSocketMessage) encodedMessage))
                .thenReturn(true);
        } else {
            return exchange
                .send(DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, encodedMessage.getPayload()))
                .thenReturn(true);
        }

    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.WebSocket;
    }

    @Override
    public void close() {
        exchange.close()
                .subscribe();
    }

    @Override
    public void ping() {
    }

    @Override
    public boolean isAlive() {
        return exchange.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        exchange.closeHandler(call);
    }

    public InetSocketAddress getAddress() {
        return exchange.getRemoteAddress().orElse(null);
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeout = timeout;
        exchange.setKeepAliveTimeout(timeout);
    }

    @Override
    public Duration getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public WebSocketDeviceSession copy() {
        WebSocketDeviceSession session = new WebSocketDeviceSession(operator, exchange);

        session.setKeepAliveTimeout(keepAliveTimeout);
        return session;
    }
}
