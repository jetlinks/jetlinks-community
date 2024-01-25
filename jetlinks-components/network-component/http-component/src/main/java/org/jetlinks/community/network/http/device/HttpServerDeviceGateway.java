package org.jetlinks.community.network.http.device;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.route.HttpRoute;
import org.jetlinks.core.route.WebsocketRoute;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.server.HttpExchange;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.community.network.http.server.WebSocketExchange;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Http 服务设备网关，使用指定的协议包，将网络组件中Http服务的请求处理为设备消息
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class HttpServerDeviceGateway extends AbstractDeviceGateway {

    final HttpServer httpServer;

    @Setter
    Mono<ProtocolSupport> protocol;

    private final DeviceRegistry registry;

    private final DeviceGatewayHelper helper;

    private final Map<RouteKey, Disposable> handlers = new ConcurrentHashMap<>();
    private final Map<String, Disposable> websocketHandlers = new ConcurrentHashMap<>();


    public HttpServerDeviceGateway(String id,
                                   HttpServer server,
                                   Mono<ProtocolSupport> protocol,
                                   DeviceSessionManager sessionManager,
                                   DeviceRegistry registry,
                                   DecodedClientMessageHandler messageHandler) {
        super(id);
        this.httpServer = server;
        this.registry = registry;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, messageHandler);
        this.protocol = protocol;
    }


    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }
    private Disposable handleRequest(HttpMethod method, String url) {
        return httpServer
            .handleRequest(method, url)
            //todo 背压处理
            .flatMap(this::handleHttpRequest, Integer.MAX_VALUE)
            .subscribe();
    }

    private Disposable handleWebsocketRequest(String url) {
        return httpServer
            .handleWebsocket(url)
            //todo 背压处理
            .flatMap(this::handleWebsocketRequest, Integer.MAX_VALUE)
            .subscribe();
    }


    private Mono<Void> handleWebsocketRequest(WebSocketExchange exchange) {

        return protocol
            .flatMap(protocol -> protocol
                .authenticate(WebsocketAuthenticationRequest.of(exchange), registry)
                .onErrorResume(err -> Mono.just(AuthenticationResponse.error(500, err.getMessage())))
                .flatMap(result -> {
                    if (result.isSuccess()) {
                        String deviceId = result.getDeviceId();
                        if (StringUtils.hasText(deviceId)) {
                            DeviceOnlineMessage message = new DeviceOnlineMessage();
                            message.setDeviceId(deviceId);
                            return this
                                .handleWebsocketMessage(message, exchange, null)
                                .flatMap(device -> exchange
                                    .receive()
                                    .flatMap(msg -> handleWebsocketRequest(exchange, msg, device))
                                    .then());
                        } else {
                            return exchange
                                .receive()
                                .flatMap(msg -> handleWebsocketRequest(exchange, msg, null))
                                .then();
                        }
                    } else {
                        log.warn("设备[{}] Websocket 认证失败:{}", exchange
                            .getRemoteAddress()
                            .orElse(null), result.getMessage());
                        return exchange.close(HttpStatus.UNAUTHORIZED);
                    }
                }));

    }

    private Mono<Void> handleWebsocketRequest(WebSocketExchange exchange, WebSocketMessage msg, DeviceOperator device) {
        if (!isStarted()) {
            return exchange
                .close(WebSocketCloseStatus.BAD_GATEWAY.code())
                .onErrorResume((error) -> {
                    log.error(error.getMessage(), error);
                    return Mono.empty();
                });
        }
        WebSocketDeviceSession session = new WebSocketDeviceSession(device, exchange);

        return protocol
            .flatMap(protocol -> {
                if (log.isDebugEnabled()) {
                    log.debug("收到HTTP请求\n{}", msg);
                }
                //调用协议执行解码
                return protocol
                    .getMessageCodec(DefaultTransport.WebSocket)
                    .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(
                        session, msg, registry, deviceMessage -> handleWebsocketMessage(deviceMessage, exchange, session).then())))
                    .cast(DeviceMessage.class)
                    .concatMap(deviceMessage -> handleWebsocketMessage(deviceMessage, exchange, session))
                    .doOnNext(session::setOperator)
                    .onErrorResume(err -> {
                        log.error("处理http请求失败:\n{}", msg, err);
                        return exchange
                            .close(HttpStatus.BAD_REQUEST)
                            .then(Mono.empty());
                    })
                    .then();
            })
            .as(MonoTracer.create("http-device-gateway/" + getId() + exchange.getPath()))
            .onErrorResume((error) -> {
                log.error(error.getMessage(), error);
                return exchange
                    .close(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .then(Mono.empty());
            });
    }

    private Mono<DeviceOperator> handleWebsocketMessage(DeviceMessage message,
                                                        WebSocketExchange exchange,
                                                        WebSocketDeviceSession session) {
        monitor.receivedMessage();

        if (null != session && !StringUtils.hasText(message.getDeviceId())) {
            message.thingId(DeviceThingType.device, session.getDeviceId());
        }

        return helper
            .handleDeviceMessage(
                message,
                device -> new WebSocketDeviceSession(device, exchange),
                deviceSession -> {
                    if (deviceSession.isWrapFrom(WebSocketDeviceSession.class)) {
                        deviceSession
                            .unwrap(WebSocketDeviceSession.class)
                            .setExchange(exchange);
                    } else if (deviceSession.isWrapFrom(HttpDeviceSession.class)) {
                        deviceSession
                            .unwrap(HttpDeviceSession.class)
                            .setWebsocket(exchange);
                    }
                },
                () -> exchange
                    .close(HttpStatus.NOT_FOUND)
                    .then(Mono.empty()));
    }

    private Mono<Void> handleHttpRequest(HttpExchange exchange) {
        if (!isStarted()) {
            return exchange
                .error(HttpStatus.SERVICE_UNAVAILABLE)
                .onErrorResume((error) -> {
                    log.error(error.getMessage(), error);
                    return Mono.empty();
                });
        }
        return protocol
            .flatMap(protocol -> exchange
                .toExchangeMessage()
                .flatMap(httpMessage -> {
                    if (log.isDebugEnabled()) {
                        log.debug("收到HTTP请求\n{}", httpMessage);
                    }
                    InetSocketAddress address = exchange.request().getClientAddress();
                    UnknownHttpDeviceSession session = new UnknownHttpDeviceSession(exchange);
                    //调用协议执行解码
                    return protocol
                        .getMessageCodec(getTransport())
                        .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(
                            session, httpMessage, registry, msg -> handleMessage(msg, exchange, httpMessage))))
                        .cast(DeviceMessage.class)
                        .concatMap(deviceMessage -> handleMessage(deviceMessage, exchange, httpMessage))
                        .then(Mono.defer(() -> {
                            //如果协议包里没有回复，那就响应200
                            if (!exchange.isClosed()) {
                                return exchange.ok();
                            }
                            return Mono.empty();
                        }))
                        .onErrorResume(err -> {
                            log.error("处理http请求失败:\n{}", httpMessage, err);
                            return response500Error(exchange, err);
                        })
                        .then();
                }))
            .as(MonoTracer.create("http-device-gateway/" + getId() + exchange.request().getPath()))
            .onErrorResume((error) -> {
                log.error(error.getMessage(), error);
                return response500Error(exchange, error);
            });
    }

    private Mono<Void> handleMessage(DeviceMessage deviceMessage,
                                     HttpExchange exchange,
                                     HttpExchangeMessage message) {
        InetSocketAddress address = exchange
            .request()
            .getClientAddress();
        monitor.receivedMessage();
        return helper
            .handleDeviceMessage(deviceMessage,
                                 device -> new HttpDeviceSession(device, address),
                                 ignore -> {
                                 },
                                 () -> {
                                     log.warn("无法从HTTP消息中获取设备信息:\n{}\n\n设备消息:{}", message, deviceMessage);
                                     return exchange
                                         .error(HttpStatus.NOT_FOUND)
                                         .then(Mono.empty());
                                 })
            .then();
    }

    private void doReloadRoute(List<HttpRoute> routes) {

        Map<RouteKey, Disposable> readyToRemove = new HashMap<>(handlers);
        for (HttpRoute route : routes) {
            for (HttpMethod httpMethod : route.getMethod()) {
                String addr = TopicUtils
                    .convertToMqttTopic(route.getAddress())
                    .replace("+", "*")
                    .replace("#", "**");

                RouteKey key = RouteKey.of(httpMethod, addr);
                readyToRemove.remove(key);
                handlers.computeIfAbsent(key, _key -> handleRequest(_key.method, _key.url));
            }
        }
        //取消处理被移除的url信息
        for (Disposable value : readyToRemove.values()) {
            value.dispose();
        }

    }

    private void doReloadRouteWebsocket(List<WebsocketRoute> routes) {

        Map<String, Disposable> readyToRemove = new HashMap<>(websocketHandlers);
        for (WebsocketRoute route : routes) {
            String addr = TopicUtils
                .convertToMqttTopic(route.getAddress())
                .replace("+", "*")
                .replace("#", "**");

            readyToRemove.remove(addr);
            websocketHandlers.computeIfAbsent(addr, this::handleWebsocketRequest);
        }
        //取消处理被移除的url信息
        for (Disposable value : readyToRemove.values()) {
            value.dispose();
        }

    }

    final Mono<Void> reloadHttp() {
        return protocol
            .flatMap(support -> support
                .getRoutes(DefaultTransport.HTTP)
                .filter(HttpRoute.class::isInstance)
                .cast(HttpRoute.class)
                .collectList()
                .doOnEach(ReactiveLogger
                              .onNext(routes -> {
                                  //协议包里没有配置Http url信息
                                  if (CollectionUtils.isEmpty(routes)) {
                                      log.warn("The protocol [{}] is not configured with url information", support.getId());
                                  }
                              }))
                .doOnNext(this::doReloadRoute))
            .then();
    }

    final Mono<Void> reloadWebsocket() {
        return protocol
            .flatMap(support -> support
                .getRoutes(DefaultTransport.WebSocket)
                .filter(WebsocketRoute.class::isInstance)
                .cast(WebsocketRoute.class)
                .collectList()
                .doOnNext(this::doReloadRouteWebsocket))
            .then();
    }

    final Mono<Void> reload() {

        return reloadHttp()
            .then(reloadWebsocket());
    }


    @AllArgsConstructor(staticName = "of")
    private static class RouteKey {
        private HttpMethod method;
        private String url;
    }

    private Mono<Void> response500Error(HttpExchange exchange, Throwable err) {
        return exchange.error(HttpStatus.INTERNAL_SERVER_ERROR, err);
    }

    @Override
    protected Mono<Void> doStartup() {
        return reload();
    }

    @Override
    protected Mono<Void> doShutdown() {
        for (Disposable value : handlers.values()) {
            value.dispose();
        }
        handlers.clear();
        return Mono.empty();
    }
}
