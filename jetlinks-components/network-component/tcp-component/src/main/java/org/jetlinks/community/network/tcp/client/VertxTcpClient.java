package org.jetlinks.community.network.tcp.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class VertxTcpClient extends AbstractTcpClient {

    public volatile NetClient client;

    public volatile NetSocket socket;

    volatile PayloadParser payloadParser;

    @Getter
    private final String id;

    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(10).toMillis();

    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();

    @Override
    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeoutMs = timeout.toMillis();
    }

    @Override
    public boolean isAlive() {
        return socket != null && (keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs);
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    public VertxTcpClient(String id) {
        this.id = id;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close tcp client error", e);
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (null == socket) {
            return null;
        }
        SocketAddress socketAddress = socket.remoteAddress();
        return new InetSocketAddress(socketAddress.host(), socketAddress.port());
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.TCP_CLIENT;
    }

    @Override
    public void shutdown() {
        log.debug("tcp client [{}] disconnect", getId());
        if (null != client) {
            execute(client::close);
            client = null;
        }
        if (null != socket) {
            execute(socket::close);
            socket = null;
        }
        if (null != payloadParser) {
            execute(payloadParser::close);
            payloadParser = null;
        }
        for (Runnable runnable : disconnectListener) {
            execute(runnable);
        }
        disconnectListener.clear();
    }

    public void setClient(NetClient client) {
        if (this.client != null && this.client != client) {
            this.client.close();
        }
        keepAlive();
        this.client = client;
    }

    public void setRecordParser(PayloadParser payloadParser) {
        if (null != this.payloadParser && this.payloadParser != payloadParser) {
            this.payloadParser.close();
        }
        this.payloadParser = payloadParser;
        this.payloadParser
            .handlePayload()
            .onErrorContinue((err, res) -> log.error(err.getMessage(), err))
            .subscribe(buffer -> received(new TcpMessage(buffer.getByteBuf())));
    }

    public void setSocket(NetSocket socket) {
        Objects.requireNonNull(payloadParser);
        if (this.socket != null && this.socket != socket) {
            this.socket.close();
        }
        this.socket = socket;
        this.socket.closeHandler(v -> {
            shutdown();
        });
        this.socket.handler(buffer -> {
            keepAlive();
            payloadParser.handle(buffer);
            if (this.socket != socket) {
                log.warn("tcp client [{}] memory leak ", socket.remoteAddress());
                socket.close();
            }
        });
    }

    @Override
    public Mono<Boolean> send(TcpMessage message) {
        return Mono.<Boolean>create((sink) -> {
            if (socket == null) {
                sink.error(new SocketException("socket closed"));
                return;
            }
            socket.write(Buffer.buffer(message.getPayload()), r -> {
                keepAlive();
                if (r.succeeded()) {
                    sink.success(true);
                } else {
                    sink.error(r.cause());
                }
            });
        });
    }

    @Override
    public void onDisconnect(Runnable disconnected) {
        disconnectListener.add(disconnected);
    }
}
