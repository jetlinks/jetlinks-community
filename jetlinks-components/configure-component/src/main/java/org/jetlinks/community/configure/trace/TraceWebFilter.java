package org.jetlinks.community.configure.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.hswebframework.web.logging.AccessLoggerInfo;
import org.hswebframework.web.logging.events.AccessLoggerBeforeEvent;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class TraceWebFilter implements WebFilter, Ordered {

    static AttributeKey<String> action = AttributeKey.stringKey("request.action");
    static AttributeKey<String> ip = AttributeKey.stringKey("request.ip");
    static AttributeKey<String> userId = AttributeKey.stringKey("user.id");
    static AttributeKey<String> username = AttributeKey.stringKey("user.username");
    static AttributeKey<String> userName = AttributeKey.stringKey("user.name");

    @SuppressWarnings("all")
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             WebFilterChain chain) {
        String spanName = exchange.getRequest().getMethodValue() +
            ":" + exchange.getRequest().getPath().value();

        return TraceHolder
            //将追踪信息返回到响应头
            .writeContextTo(exchange.getResponse().getHeaders(), HttpHeaders::set)
            .then(chain.filter(exchange))
            .as(MonoTracer.create(spanName))
            .as(MonoTracer.createWith(exchange.getRequest().getHeaders().toSingleValueMap()));

    }

    public static final Authentication anonymous;

    static {
        SimpleAuthentication auth = new SimpleAuthentication();
        auth.setUser(SimpleUser
                         .builder()
                         .id("anonymous")
                         .userType("none")
                         .name("anonymous")
                         .username("anonymous")
                         .build());
        anonymous = auth;
    }

    @EventListener
    public void handleAccessLogger(AccessLoggerBeforeEvent event) {
        String spanName = "/java/" + event.getLogger().getTarget().getSimpleName()
            + "/" + event.getLogger().getMethod().getName();

        event.transformFirst(first -> Authentication
            .currentReactive()
            .defaultIfEmpty(anonymous)
            .flatMap(auth -> first
                .thenReturn(event.getLogger())
                .as(MonoTracer.<AccessLoggerInfo>create(
                    spanName,
                    (span, log) -> {
                        if (log.getException() != null) {
                            span.recordException(log.getException());
                            span.setStatus(StatusCode.ERROR);
                        }
                        span.setAttribute(action, log.getAction());
                        span.setAttribute(ip, log.getIp());
                        span.setAttribute(userId, auth.getUser().getId());
                        span.setAttribute(username, auth.getUser().getUsername());
                        span.setAttribute(userName, auth.getUser().getName());
                        log
                            .getContext()
                            .put("traceId", span.getSpanContext().getTraceId());
                    },
                    builder -> builder
                        .setStartTimestamp(event.getLogger().getRequestTime(),
                                           TimeUnit.MILLISECONDS))
                ))
        );
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 100;
    }
}
