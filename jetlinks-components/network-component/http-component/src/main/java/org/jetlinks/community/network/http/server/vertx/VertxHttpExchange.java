package org.jetlinks.community.network.http.server.vertx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.SocketAddress;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.core.message.codec.http.MultiPart;
import org.jetlinks.community.network.http.DefaultHttpRequestMessage;
import org.jetlinks.community.network.http.VertxWebUtils;
import org.jetlinks.community.network.http.server.HttpExchange;
import org.jetlinks.community.network.http.server.HttpRequest;
import org.jetlinks.community.network.http.server.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认HTTP交换消息
 *
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Slf4j
public class VertxHttpExchange implements HttpExchange, HttpResponse, HttpRequest {

    static final AtomicReferenceFieldUpdater<VertxHttpExchange, Boolean> ALREADY_RESPONSE = AtomicReferenceFieldUpdater
        .newUpdater(VertxHttpExchange.class, Boolean.class, "alreadyResponse");

    static final MultiPart emptyPart = MultiPart.of(Collections.emptyList());

    private final HttpServerRequest httpServerRequest;

    private final HttpServerResponse response;

    private final Mono<ByteBuf> body;

    private final String requestId;

    private MultiPart multiPart;

    private volatile Boolean alreadyResponse = false;

    public VertxHttpExchange(HttpServerRequest httpServerRequest,
                             HttpServerConfig config) {

        this.httpServerRequest = httpServerRequest;
        this.response = httpServerRequest.response();
        this.requestId = UUID.randomUUID().toString();
        config.getHttpHeaders().forEach(response::putHeader);

        if (httpServerRequest.method() == HttpMethod.GET) {
            body = Mono.just(Unpooled.EMPTY_BUFFER);
        } else {

            Mono<ByteBuf> buffer = Mono
                .fromCompletionStage(this.httpServerRequest.body().toCompletionStage())
                .map(Buffer::getByteBuf);

            if (MultiPart.isMultiPart(getContentType())) {
                body = MultiPart
                    .parse(getSpringHttpHeaders(), buffer.flux())
                    .doOnNext(this::setMultiPart)
                    .thenReturn(Unpooled.EMPTY_BUFFER)
                    .cache();
            } else {
                body = buffer;
            }

        }
    }

    @Override
    @Generated
    public String requestId() {
        return requestId;
    }

    @Override
    @Generated
    public long timestamp() {
        return System.currentTimeMillis();
    }

    @Override
    @Generated
    public HttpRequest request() {
        return this;
    }


    @Override
    @Generated
    public HttpResponse response() {
        return this;
    }

    @Override
    @Generated
    public boolean isClosed() {
        return response.closed() || response.ended();
    }

    @Override
    @Generated
    public HttpResponse status(int status) {
        response.setStatusCode(status);
        return this;
    }

    static Map<String, String> convertRequestParam(MultiMap multiMap) {
        return multiMap
            .entries()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> String.join(",", a, b)));
    }

    static List<Header> convertHeader(MultiMap multiMap) {
        return multiMap
            .names()
            .stream()
            .map(name -> {
                Header header = new Header();
                header.setName(name);
                header.setValue(multiMap.getAll(name).toArray(new String[0]));
                return header;
            })
            .collect(Collectors.toList())
            ;
    }

    private org.springframework.http.HttpMethod convertMethodType(HttpMethod method) {
        for (org.springframework.http.HttpMethod httpMethod : org.springframework.http.HttpMethod.values()) {
            if (httpMethod.toString().equals(method.toString())) {
                return httpMethod;
            }
        }
        throw new UnsupportedOperationException("不支持的HttpMethod类型: " + method);
    }

    private void setResponseDefaultLength(int length) {
        if (!isClosed()) {
            response.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        }
    }

    @Override
    public HttpResponse contentType(MediaType mediaType) {
        if (null != mediaType && !isClosed()) {
            response.putHeader(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        }
        return this;
    }

    @Override
    public HttpResponse header(Header header) {
        if (null != header && !isClosed()) {
            response.putHeader(header.getName(), Arrays.<String>asList(header.getValue()));
        }
        return this;
    }

    @Override
    public HttpResponse header(String header, String value) {
        if (header != null && value != null && !isClosed()) {
            response.putHeader(header, value);
        }
        return this;
    }

    @Override
    public Mono<Void> write(ByteBuf buffer) {
        if (isClosed()) {
            return Mono.empty();
        }
        return Mono
            .<Void>create(sink -> {
                Buffer buf = Buffer.buffer(buffer);
                setResponseDefaultLength(buf.length());
                response.write(buf, v -> {
                    sink.success();
                    if (!(buffer instanceof UnpooledHeapByteBuf)) {
                        ReferenceCountUtil.safeRelease(buffer);
                    }
                });
            });
    }

    @Override
    public Mono<Void> end() {
        if (isClosed()) {
            return Mono.empty();
        }
        ALREADY_RESPONSE.set(this, true);
        return Mono
            .<Void>create(sink -> {
                if (response.ended()) {
                    sink.success();
                    return;
                }
                response.end(v -> sink.success());
            });
    }

    @Override
    public Mono<Void> response(HttpResponseMessage message) {
        if (ALREADY_RESPONSE.compareAndSet(this, false, true)) {
            return HttpExchange.super.response(message);
        } else {
            if (log.isInfoEnabled()) {
                log.info("http already response,discard message: {}", message.print());
            }
            return Mono.empty();
        }
    }

    @Override
    @Generated
    public String getUrl() {
        return httpServerRequest.path();
    }

    @Override
    @Generated
    public String getPath() {
        return httpServerRequest.path();
    }

    @Override
    @Generated
    public String getRemoteIp() {
        return httpServerRequest.remoteAddress().host();
    }

    @Override
    @Generated
    public String getRealIp() {
        return VertxWebUtils.getIpAddr(httpServerRequest);
    }

    @Override
    public InetSocketAddress getClientAddress() {
        SocketAddress address = httpServerRequest.remoteAddress();
        if (null == address) {
            return null;
        }
        return new InetSocketAddress(getRealIp(), address.port());
    }

    @Override
    public MediaType getContentType() {
        String contentType = httpServerRequest.getHeader(HttpHeaders.CONTENT_TYPE);
        if (StringUtils.hasText(contentType)) {
            return MediaType.parseMediaType(contentType);
        } else {
            return MediaType.APPLICATION_FORM_URLENCODED;
        }
    }

    @Override
    public Optional<String> getQueryParameter(String key) {
        return Optional.ofNullable(httpServerRequest.getParam(key));
    }

    @Override
    public Map<String, String> getQueryParameters() {
        Map<String, String> params = new HashMap<>();

        MultiMap map = httpServerRequest.params();

        for (String name : map.names()) {
            params.put(name, String.join(",", map.getAll(name)));
        }

        return params;
    }

    @Override
    @Generated
    public Map<String, String> getRequestParam() {
        return convertRequestParam(httpServerRequest.formAttributes());
    }

    @Override
    @Generated
    public Mono<ByteBuf> getBody() {
        return body;
    }

    @Override
    @Generated
    public org.springframework.http.HttpMethod getMethod() {
        return convertMethodType(httpServerRequest.method());
    }

    @Override
    @Generated
    public List<Header> getHeaders() {
        return convertHeader(httpServerRequest.headers());
    }

    private HttpHeaders getSpringHttpHeaders() {
        MultiMap map = httpServerRequest.headers();
        HttpHeaders headers = new HttpHeaders();
        for (String name : map.names()) {
            headers.addAll(name, map.getAll(name));
        }
        return headers;
    }

    @Override
    public Optional<Header> getHeader(String key) {
        return Optional.ofNullable(
            getHeaders()
                .stream()
                .collect(Collectors.toMap(Header::getName, Function.identity()))
                .get(key));
    }

    @Override
    public Mono<HttpRequestMessage> toMessage() {
        return this.getBody()
                   .defaultIfEmpty(Unpooled.EMPTY_BUFFER)
                   .map(byteBuf -> {
                       DefaultHttpRequestMessage message = new DefaultHttpRequestMessage();
                       message.setContentType(this.getContentType());
                       message.setHeaders(this.getHeaders());
                       message.setMethod(this.getMethod());
                       message.setPayload(byteBuf);
                       message.setQueryParameters(this.getQueryParameters());
                       message.setUrl(this.getUrl());
                       message.setMultiPart(multiPart);
                       return message;
                   });
    }

    @Override
    public Mono<MultiPart> multiPart() {
        return body
            .then(Mono.fromSupplier(this::getMultiPart))
            .defaultIfEmpty(emptyPart);
    }
}
