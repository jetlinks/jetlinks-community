package org.jetlinks.community.network.http.device;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.http.*;
import org.jetlinks.community.network.http.server.HttpExchange;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.stream.Collectors;


@Slf4j
public class AsyncHttpExchangeMessage implements HttpExchangeMessage {

    private static final AtomicReferenceFieldUpdater<AsyncHttpExchangeMessage, Boolean>
        RESPONDED = AtomicReferenceFieldUpdater.newUpdater(AsyncHttpExchangeMessage.class, Boolean.class, "responded");

    private final HttpExchange exchange;

    private volatile Boolean responded = false;

    public AsyncHttpExchangeMessage(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Nonnull
    @Override
    public Mono<Void> response(@Nonnull HttpResponseMessage message) {
        return Mono
            .defer(() -> {
                if (!RESPONDED.getAndSet(this, true) && !exchange.isClosed()) {
                    if (log.isDebugEnabled()) {
                        log.debug("响应HTTP请求:\n{}", message.print());
                    }
                    return exchange.response(message);
                }
                return Mono.empty();
            });
    }

    @Override
    public Mono<ByteBuf> payload() {
        return exchange
            .request()
            .getBody();
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

    @Override
    public Mono<MultiPart> multiPartAsync() {
        return exchange.request().multiPart();
    }

    @Override
    @Deprecated
    @Nonnull
    public Optional<MultiPart> multiPart() {
        //log.warn("该方法已过时,请使用multiPartAsync()方法替代");
        return Optional.ofNullable(
            multiPartAsync()
                .toFuture()
                .getNow(null)
        );
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        //log.warn("该方法已过时,请使用payload()方法替代");
        return payload()
            .toFuture()
            .getNow(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public String toString() {
        return print();
    }

    @Override
    public String print() {
        StringBuilder builder = new StringBuilder();
        builder.append(getMethod()).append(" ").append(getPath());
        if (!CollectionUtils.isEmpty(getQueryParameters())) {
            builder.append("?")
                   .append(getQueryParameters()
                               .entrySet().stream()
                               .map(e -> e.getKey().concat("=").concat(e.getValue()))
                               .collect(Collectors.joining("&")))
                   .append("\n");
        } else {
            builder.append("\n");
        }
        for (Header header : getHeaders()) {
            builder
                .append(header.getName()).append(": ").append(String.join(",", header.getValue()))
                .append("\n");
        }

        if (MultiPart.isMultiPart(getContentType())) {
            MultiPart multiPart = multiPartAsync()
                .toFuture()
                .getNow(null);
            builder.append("\n");
            if (multiPart != null) {
                builder.append("\n");
                for (Part part : multiPart.getParts()) {
                    builder.append(part).append("\n");
                }
            } else {
                builder.append("\n")
                       .append("<unread multiPart>\n");
            }
        } else if (getMethod() != HttpMethod.GET && getMethod() != HttpMethod.DELETE) {
            ByteBuf payload = payload().toFuture().getNow(null);
            if (payload == null) {
                return builder.append("\n")
                              .append("<unread payload>\n")
                              .toString();
            }
            if(payload.refCnt()==0){
                return builder.append("\n")
                              .append("<payload released>\n")
                              .toString();
            }
            if (payload.readableBytes() == 0) {
                return builder.toString();
            }
            builder.append("\n");
            if (ByteBufUtil.isText(payload, StandardCharsets.UTF_8)) {
                builder.append(payload.toString(StandardCharsets.UTF_8));
            } else {
                ByteBufUtil.appendPrettyHexDump(builder, payload);
            }
        }

        return builder.toString();
    }
}
