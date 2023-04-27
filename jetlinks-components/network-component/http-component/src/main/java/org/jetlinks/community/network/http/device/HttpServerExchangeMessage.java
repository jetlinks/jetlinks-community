package org.jetlinks.community.network.http.device;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.core.message.codec.http.MultiPart;
import org.jetlinks.community.network.http.server.HttpExchange;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class HttpServerExchangeMessage implements HttpExchangeMessage {


    AtomicReference<Boolean> responded = new AtomicReference<>(false);

    MultiPart multiPart;

    public HttpServerExchangeMessage(HttpExchange exchange,
                                     ByteBuf payload,
                                     MultiPart multiPart) {
        this.exchange = exchange;
        this.payload = payload;
        this.multiPart = multiPart;
    }

    private final HttpExchange exchange;
    private final ByteBuf payload;

    @Nonnull
    @Override
    public Mono<Void> response(@Nonnull HttpResponseMessage message) {
        return Mono
            .defer(() -> {
                if (!responded.getAndSet(true) && !exchange.isClosed()) {
                    if (log.isDebugEnabled()) {
                        log.debug("响应HTTP请求:\n{}", message.print());
                    }
                    return exchange.response(message);
                }
                return Mono.empty();
            });
    }

    @Override
    public Optional<MultiPart> multiPart() {
        return Optional
            .ofNullable(multiPart)
            .filter(part -> part.getParts().size() > 0);
    }

    @Nonnull
    @Override
    public String getUrl() {
        return exchange.request().getUrl();
    }

    @Nonnull
    @Override
    public HttpMethod getMethod() {
        return exchange.request().getMethod();
    }

    @Nullable
    @Override
    public MediaType getContentType() {
        return exchange.request().getContentType();
    }

    @Nonnull
    @Override
    public List<Header> getHeaders() {
        return exchange.request().getHeaders();
    }

    @Nullable
    @Override
    public Map<String, String> getQueryParameters() {
        return exchange.request().getQueryParameters();
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return print();
    }
}
