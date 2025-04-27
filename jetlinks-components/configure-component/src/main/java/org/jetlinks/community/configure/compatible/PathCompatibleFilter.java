package org.jetlinks.community.configure.compatible;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 兼容处理路径结尾为/的请求
 *
 * @since 2.10
 */
public class PathCompatibleFilter implements WebFilter {
    @Override
    @Nonnull
    public Mono<Void> filter(ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        if (!path.equals("/") && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
            return chain.filter(
                exchange
                    .mutate()
                    .request(exchange
                                 .getRequest()
                                 .mutate()
                                 .path(path)
                                 .build())
                    .build());
        }
        return chain.filter(exchange);
    }
}
