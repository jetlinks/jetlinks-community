/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.network.http.server.vertx;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.vertx.core.http.HttpClosedException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.topic.Topic;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.server.HttpExchange;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.community.network.http.server.WebSocketExchange;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class VertxHttpServer implements HttpServer {

    private static final SharedPathString WEBSOCKET_PREFIX = SharedPathString.of("/ws");

    private static final Map<HttpMethod, SeparatedCharSequence> HTTP_PREFIX_CACHE = new ConcurrentHashMap<>();

    private Collection<io.vertx.core.http.HttpServer> httpServers;

    private HttpServerConfig config;

    private String id;

    private static final AtomicLongFieldUpdater<VertxHttpServer> PENDING =
        AtomicLongFieldUpdater.newUpdater(VertxHttpServer.class, "pending");
    private volatile long pending;

    private int maxConcurrency = -1;

    private final Topic<FluxSink<HttpExchange>> route = Topic.createRoot();
    private final Topic<FluxSink<WebSocketExchange>> websocketRoute = Topic.createRoot();

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    public VertxHttpServer(HttpServerConfig config) {
        this.config = config;
        this.id = config.getId();
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    public void setHttpServers(Collection<io.vertx.core.http.HttpServer> httpServers) {
        if (isAlive()) {
            shutdown();
        }
        this.httpServers = httpServers;
        for (io.vertx.core.http.HttpServer server : this.httpServers) {
            server
                .webSocketHandler(socket -> {
                    long pending = PENDING.incrementAndGet(this);
                    if (maxConcurrency > 0 && pending >= maxConcurrency) {
                        PENDING.decrementAndGet(this);
                        socket.close((short) WebSocketCloseStatus.TRY_AGAIN_LATER.code());
                        return;
                    }

                    socket.exceptionHandler(err -> {
                        if(err instanceof HttpClosedException){
                            return;
                        }
                        log.warn(err.getMessage(), err);
                    });
                    VertxWebSocketExchange exchange = new VertxWebSocketExchange(
                        socket
                    );
                    exchange.closeHandler(() -> PENDING.decrementAndGet(this));
                    Topic.find(
                        WEBSOCKET_PREFIX.append(parsePath(exchange.getPath())),
                        websocketRoute,
                        exchange,
                        socket,
                        null,
                        null,
                        (_exchange, _req, nil, nil2, topic) -> {
                            for (FluxSink<WebSocketExchange> sink : topic.getSubscribers()) {
                                _exchange.mark();
                                sink.next(_exchange);
                            }
                        }, (_exchange, _socket, nil2, nil) -> {
                            if (_exchange.handleCount() == 0) {
                                if (log.isInfoEnabled()) {
                                    log.info("http server no handler for:[{}://{}{}] remote: {}",
                                             _socket.scheme(),
                                             _socket.authority(),
                                             _socket.path(),
                                             _socket.remoteAddress());
                                }
                                _socket.close((short) WebSocketCloseStatus.ENDPOINT_UNAVAILABLE.code());
                            }
                        });

                })
                .requestHandler(request -> {

                    request.endHandler(ignore -> PENDING.decrementAndGet(this));

                    long pending = PENDING.incrementAndGet(this);
                    if (maxConcurrency > 0 && pending >= maxConcurrency) {
                        request
                            .response()
                            .setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                            .end();
                        return;
                    }
                    request.exceptionHandler(err -> {
                        if(err instanceof HttpClosedException){
                            return;
                        }
                        log.warn(err.getMessage(), err);
                    });
                    try {
                        VertxHttpExchange exchange = new VertxHttpExchange(request, config);
                        Topic.find(
                            getHttpPrefix(exchange.request().getMethod())
                                .append(parsePath(exchange.getPath())),
                            route,
                            exchange,
                            request,
                            null,
                            null,
                            //查找到订阅者
                            (_exchange,
                             _req,
                             nil, nil2,
                             topic) -> {
                                for (FluxSink<HttpExchange> sink : topic.getSubscribers()) {
                                    _exchange.mark();
                                    sink.next(_exchange);
                                }
                            },
                            //全部查找结束
                            (_exchange,
                             _req,
                             nil2, nil) -> {
                                if (_exchange.handleCount() == 0) {
                                    if (log.isInfoEnabled()) {
                                        log.info("http server no handler for:[{} {}://{}:{}] remote: {}",
                                                 _req.method(),
                                                 _req.scheme(),
                                                 _req.authority(),
                                                 _req.path(),
                                                 _req.remoteAddress());
                                    }
                                    _req
                                        .response()
                                        .setStatusCode(HttpStatus.NOT_FOUND.value())
                                        .end();
                                }
                            });
                    } catch (Throwable e) {
                        request
                            .response()
                            .setStatusCode(HttpStatus.BAD_GATEWAY.value())
                            .end();
                    }

                });
            server.exceptionHandler(err -> {
                // 忽略
                if (err instanceof HttpClosedException) {
                    return;
                }
                log.warn("http server [{}] error", bindAddress, err);
            });
        }
    }

    private SeparatedCharSequence parsePath(String url) {
        while (url.charAt(url.length() - 1) == '/') {
            url = url.substring(0, url.length() - 1);
        }
        String[] split = TopicUtils.split(
            url,
            '/',
            (i, p) -> {
                //不支持通配符
                if ("*".equals(p) || "**".equals(p)) {
                    throw new I18nSupportException.NoStackTrace("error.illegal_http_path");
                }
                return p;
            });
        return SharedPathString.of(split);
    }

    private SeparatedCharSequence getHttpPrefix(HttpMethod method) {
        return HTTP_PREFIX_CACHE.computeIfAbsent(
            method,
            m -> {
                String prefix = m.name().toLowerCase();
                return SharedPathString.of("/" + prefix);
            });
    }


    @Override
    public Flux<HttpExchange> handleRequest() {
        return handleRequest("*", "/**");
    }

    @Override
    public Flux<WebSocketExchange> handleWebsocket(String urlPattern) {
        return createRoute(websocketRoute, "ws", urlPattern);
    }

    @Override
    public Flux<HttpExchange> handleRequest(String method, String... urlPatterns) {
        return createRoute(route, method.toLowerCase(), urlPatterns);
    }

    private <T> Flux<T> createRoute(Topic<FluxSink<T>> root, String prefix, String... urlPatterns) {
        return Flux.create(sink -> {
            Disposable.Composite disposable = Disposables.composite();
            for (String urlPattern : urlPatterns) {
                String pattern = Stream
                    .of(urlPattern.split("/"))
                    .map(str -> {
                        //处理路径变量,如: /devices/{id}
                        if (str.startsWith("{") && str.endsWith("}")) {
                            return "*";
                        }
                        return str;
                    })
                    .collect(Collectors.joining("/"));
                if (pattern.endsWith("/")) {
                    pattern = pattern.substring(0, pattern.length() - 1);
                }
                if (!pattern.startsWith("/")) {
                    pattern = "/".concat(pattern);
                }
                pattern = "/" + prefix + pattern;
                log.debug("handle http request : {}", pattern);
                Topic<FluxSink<T>> sub = root.append(pattern);
                sub.subscribe(sink);
                disposable.add(() -> sub.unsubscribe(sink));
            }
            sink.onDispose(disposable);
        });
    }

    @Override
    @Generated
    public String getId() {
        return id;
    }

    @Override
    @Generated
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public void shutdown() {
        if (httpServers != null) {
            for (io.vertx.core.http.HttpServer httpServer : httpServers) {
                httpServer.close(res -> {
                    if (res.failed()) {
                        log.error(res.cause().getMessage(), res.cause());
                    } else {
                        log.debug("http server [{}] closed", httpServer.actualPort());
                    }
                });
            }
            httpServers.clear();
            httpServers = null;
        }
    }

    @Override
    public boolean isAlive() {
        return httpServers != null && !httpServers.isEmpty();
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
