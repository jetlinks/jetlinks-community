package org.jetlinks.community.network.http.server.vertx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.network.http.server.WebSocketExchange;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketSession;
import org.jetlinks.core.message.codec.http.websocket.WebSocketSessionMessageWrapper;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author wangzheng
 * @author zhouhao
 * @see WebSocketSession
 * @since 1.0
 */
@Slf4j
public class VertxWebSocketExchange implements WebSocketExchange {

    private final ServerWebSocket serverWebSocket;

    private final InetSocketAddress address;

    private final Sinks.Many<WebSocketMessage> sink = Reactors.createMany();

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private long keepAliveTimeOutMs = Duration.ofHours(1).toMillis();

    private long lastKeepAliveTime = System.currentTimeMillis();

    private final List<Runnable> closeHandler = new CopyOnWriteArrayList<>();

    @Getter
    private final String id;

    public VertxWebSocketExchange(ServerWebSocket serverWebSocket) {
        this.serverWebSocket = serverWebSocket;
        doReceived();
        SocketAddress socketAddress = serverWebSocket.remoteAddress();
        address = new InetSocketAddress(socketAddress.host(), socketAddress.port());
        this.id = IDGenerator.RANDOM.generate();
    }

    @Override
    public Optional<InetSocketAddress> getRemoteAddress() {
        return Optional.of(address);
    }

    private void doReceived() {
        serverWebSocket
            .textMessageHandler(text -> handle(textMessage(text)))
            .binaryMessageHandler(msg -> handle(binaryMessage(msg.getByteBuf())))
            .pongHandler(buf -> handle(pongMessage(buf.getByteBuf())))
            .closeHandler((nil) -> doClose())
            .exceptionHandler(err -> {
                if (err instanceof HttpClosedException) {
                    return;
                }
                log.error(err.getMessage(), err);
            })
        ;
    }

    private void doClose() {
        sink.emitComplete(Reactors.emitFailureHandler());
        for (Runnable runnable : closeHandler) {
            runnable.run();
        }
        closeHandler.clear();

    }

    private void handle(WebSocketMessage message) {
        this.lastKeepAliveTime = System.currentTimeMillis();
        if (sink.currentSubscriberCount() > 0) {
            sink.emitNext(WebSocketSessionMessageWrapper.of(message, this), Reactors.emitFailureHandler());
        } else {
            log.warn("websocket client[{}] session no handler", address);
        }
    }

    @Override
    public String getUri() {
        return serverWebSocket.uri();
    }

    @Override
    @Nonnull
    public List<Header> getHeaders() {
        return serverWebSocket
            .headers()
            .entries()
            .stream()
            .map(entry -> {
                Header header = new Header();
                header.setName(entry.getKey());
                header.setValue(new String[]{entry.getValue()});
                return header;
            }).collect(Collectors.toList());
    }

    @Override
    public Optional<Header> getHeader(String s) {
        return Optional.ofNullable(serverWebSocket.headers().getAll(s))
                       .map(list -> new Header(s, list.toArray(new String[0])))
            ;
    }

    @Override
    public Mono<Void> close() {
        if (serverWebSocket.isClosed()) {
            return Mono.empty();
        }
        return Mono.fromRunnable(serverWebSocket::close);
    }

    @Override
    public Mono<Void> close(int i) {
        return close(i, "Closed");
    }

    @Override
    public Mono<Void> close(int status, String reason) {
        if (serverWebSocket.isClosed()) {
            return Mono.empty();
        }
        return Mono.defer(() -> {
            short code = WebSocketCloseStatus.isValidStatusCode(status) ? (short) status : (short) WebSocketCloseStatus.BAD_GATEWAY.code();

            return Mono.fromCompletionStage(serverWebSocket
                                                .close(code, reason)
                                                .toCompletionStage());
        });
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Optional<Object> getAttribute(String s) {
        return Optional.ofNullable(attributes.get(s));
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributes.put(s, o);
    }

    @Override
    public Flux<WebSocketMessage> receive() {
        return sink.asFlux();
    }

    @Override
    public Mono<Void> send(WebSocketMessage webSocketMessage) {
        ByteBuf payload = webSocketMessage.getPayload();
        return this
            .doWrite(handler -> {
                switch (webSocketMessage.getType()) {
                    case TEXT:
                        serverWebSocket.writeTextMessage(webSocketMessage.payloadAsString(), handler);
                        return;
                    case BINARY:
                        serverWebSocket.writeBinaryMessage(Buffer.buffer(payload), handler);
                        return;
                    case PING:
                        serverWebSocket.writePing(Buffer.buffer(payload));
                        handler.handle(Future.succeededFuture());
                        return;
                }
                throw new UnsupportedOperationException("unsupported message type" + webSocketMessage.getType());
            })
            .doAfterTerminate(() -> ReferenceCountUtil.safeRelease(payload));
    }

    protected Mono<Void> doWrite(Consumer<Handler<AsyncResult<Void>>> handler) {
        this.lastKeepAliveTime = System.currentTimeMillis();
        return Mono.<Void>create(sink -> {
            try {
                handler.accept(result -> {
                    if (result.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(result.cause());
                    }
                });
            } catch (Throwable e) {
                sink.error(e);
            }
        });
    }

    @Override
    public WebSocketMessage textMessage(String s) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, Unpooled.wrappedBuffer(s.getBytes()));
    }

    @Override
    public WebSocketMessage binaryMessage(ByteBuf byteBuf) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.BINARY, byteBuf);
    }

    @Override
    public WebSocketMessage pingMessage(ByteBuf byteBuf) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.PING, byteBuf);
    }

    @Override
    public WebSocketMessage pongMessage(ByteBuf byteBuf) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.PONG, byteBuf);
    }

    @Override
    public boolean isAlive() {
        return !serverWebSocket.isClosed() &&
            (
                keepAliveTimeOutMs <= 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeOutMs
            );
    }

    @Override
    public void setKeepAliveTimeout(Duration duration) {
        this.keepAliveTimeOutMs = duration.toMillis();
        this.lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void closeHandler(Runnable handler) {
        closeHandler.add(handler);
    }

    @Override
    public long getLastKeepAliveTime() {
        return lastKeepAliveTime;
    }
}
