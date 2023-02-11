package org.jetlinks.community.network.http.server.vertx;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.topic.Topic;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.server.HttpExchange;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.community.network.http.server.WebSocketExchange;
import org.springframework.http.HttpStatus;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Collection;
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

    private Collection<io.vertx.core.http.HttpServer> httpServers;

    private HttpServerConfig config;

    private String id;

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
                    socket.exceptionHandler(err -> {
                        log.error(err.getMessage(), err);
                    });

                    String url = socket.path();
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    VertxWebSocketExchange exchange = new VertxWebSocketExchange(socket);

                    websocketRoute
                        .findTopic("/ws" + url)
                        .flatMapIterable(Topic::getSubscribers)
                        .doOnNext(sink -> sink.next(exchange))
                        .switchIfEmpty(Mono.fromRunnable(() -> {

                            log.warn("http server no handler for:[{}://{}{}]", socket.scheme(), socket.host(), socket.path());
                            socket.reject(404);

                        }))
                        .subscribe();

                })
                .requestHandler(request -> {
                    request.exceptionHandler(err -> {
                        log.error(err.getMessage(), err);
                    });

                    VertxHttpExchange exchange = new VertxHttpExchange(request, config);

                    String url = exchange.getUrl();
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }

                    route.findTopic("/" + exchange.request().getMethod().name().toLowerCase() + url)
                         .flatMapIterable(Topic::getSubscribers)
                         .doOnNext(sink -> sink.next(exchange))
                         .switchIfEmpty(Mono.fromRunnable(() -> {

                             log.warn("http server no handler for:[{} {}://{}{}]", request.method(), request.scheme(), request.host(), request.path());
                             request.response()
                                    .setStatusCode(HttpStatus.NOT_FOUND.value())
                                    .end();

                         }))
                         .subscribe();

                });
            server.exceptionHandler(err -> log.error(err.getMessage(), err));
        }
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
        return createRoute(route, method, urlPatterns);
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
