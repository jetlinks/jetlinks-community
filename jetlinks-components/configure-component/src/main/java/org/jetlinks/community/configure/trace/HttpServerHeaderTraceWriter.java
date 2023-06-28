package org.jetlinks.community.configure.trace;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.function.Consumer3;

public class HttpServerHeaderTraceWriter implements Consumer3<ServerHttpRequest.Builder, String, String> {

    public static final HttpServerHeaderTraceWriter INSTANCE = new HttpServerHeaderTraceWriter();

    @Override
    public void accept(ServerHttpRequest.Builder builder, String key, String value) {
        builder.headers(headers -> HttpHeaderTraceWriter.INSTANCE.accept(headers, key, value));
    }
}
