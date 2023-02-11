package org.jetlinks.community.network.http.server;

import org.jetlinks.community.network.ServerNetwork;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;

import java.util.Locale;

/**
 * HTTP 服务网络组件接口
 *
 * @author zhouhao
 * @since 1.0
 */
public interface HttpServer extends ServerNetwork {

    /**
     * 监听所有请求
     *
     * @return HttpExchange
     */
    Flux<HttpExchange> handleRequest();

    /**
     * 根据请求方法和url监听请求.
     * <p>
     * URL支持通配符:
     * <pre>
     *   /device/* 匹配/device/下1级的请求,如: /device/1
     *
     *   /device/** 匹配/device/下N级的请求,如: /device/1/2/3
     *
     * </pre>
     *
     * @param method     请求方法: {@link org.springframework.http.HttpMethod}
     * @param urlPattern url
     * @return HttpExchange
     */
    Flux<HttpExchange> handleRequest(String method, String... urlPattern);

    Flux<WebSocketExchange> handleWebsocket(String urlPattern);

    default Flux<HttpExchange> handleRequest(HttpMethod method, String... urlPattern) {
        return handleRequest(method.name().toLowerCase(Locale.ROOT), urlPattern);
    }

    /**
     * 停止服务
     */
    void shutdown();
}
