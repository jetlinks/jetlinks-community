package org.jetlinks.community.network.http.device;

import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.network.http.server.WebSocketExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * Http 设备会话
 *
 * @author zhouhao
 * @since 1.0
 */
class HttpDeviceSession implements DeviceSession {

    private final DeviceOperator operator;

    private final InetSocketAddress address;

    @Setter
    private WebSocketExchange websocket;

    private long lastPingTime = System.currentTimeMillis();

    //默认永不超时
    private long keepAliveTimeOutMs = -1;

    public HttpDeviceSession(DeviceOperator deviceOperator, InetSocketAddress address) {
        this.operator = deviceOperator;
        this.address = address;
    }

    @Override
    public String getId() {
        return operator.getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return operator;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return lastPingTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        //未建立websocket链接,不支持此类消息.
        if (websocket == null) {
            return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.UNSUPPORTED_MESSAGE));
        }
        if (!websocket.isAlive()) {
            return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
        }
        if (encodedMessage instanceof WebSocketMessage) {
            return websocket
                .send(((WebSocketMessage) encodedMessage))
                .thenReturn(true);
        } else {
            return websocket
                .send(DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, encodedMessage.getPayload()))
                .thenReturn(true);
        }
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.ofNullable(address);
    }

    @Override
    public void close() {
        //断开websocket连接
        if (websocket != null) {
            websocket.close().subscribe();
        }
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeOutMs = timeout.toMillis();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeOutMs <= 0
            || System.currentTimeMillis() - lastPingTime < keepAliveTimeOutMs;
    }

    @Override
    public void onClose(Runnable call) {

    }
}
