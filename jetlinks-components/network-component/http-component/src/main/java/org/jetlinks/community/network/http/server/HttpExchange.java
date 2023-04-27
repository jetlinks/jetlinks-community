package org.jetlinks.community.network.http.server;

import io.netty.buffer.Unpooled;
import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.community.network.http.device.HttpServerExchangeMessage;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.core.message.codec.http.SimpleHttpResponseMessage;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

/**
 * HTTP交换接口，支持获取请求和发送响应
 *
 * @author zhouhao
 * @since 1.0
 */
public interface HttpExchange {

    /**
     * @return 请求ID
     */
    String requestId();

    /**
     * @return 时间戳
     */
    long timestamp();

    /**
     * @return 请求接口
     */
    HttpRequest request();

    /**
     * @return 响应接口
     */
    HttpResponse response();

    /**
     * @return 是否已经完成响应
     */
    boolean isClosed();

    /**
     * 响应错误
     *
     * @param status 状态码
     * @return void
     */
    default Mono<Void> error(@NotNull HttpStatus status) {
        return response(status, "{\"message\":\"" + status.getReasonPhrase() + "\"}");
    }

    /**
     * 响应成功
     *
     * @return void
     */
    default Mono<Void> ok() {
        return response(HttpStatus.OK, "{\"message\":\"OK\"}");
    }

    /**
     * 响应指定当状态码和响应头
     *
     * @param status 状态码
     * @param body   响应体
     * @return void
     */
    default Mono<Void> response(@NotNull HttpStatus status, @NotNull String body) {
        return this.response(SimpleHttpResponseMessage
                                 .builder()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .status(status.value())
                                 .body(body.getBytes())
                                 .build());
    }

    /**
     * 根据异常信息来响应错误
     *
     * @param status 响应码
     * @param body   异常信息
     * @return void
     */
    default Mono<Void> error(@NotNull HttpStatus status, @NotNull Throwable body) {
        return response(status, body.getMessage() == null ? body.getClass().getSimpleName() : body.getMessage());
    }

    /**
     * 根据HttpResponseMessage进行响应
     *
     * @param message HttpResponseMessage
     * @return void
     */
    default Mono<Void> response(HttpResponseMessage message) {
        HttpResponse response = response();
        response.status(message.getStatus());
        if (CollectionUtils.isNotEmpty(message.getHeaders())) {
            message.getHeaders().forEach(response::header);
        }
        response.contentType(message.getContentType());
        return TraceHolder
            .writeContextTo(response, HttpResponse::header)
            .then(response.writeAndEnd(message.getPayload()))
            ;
    }

    /**
     * 转换为 HttpExchangeMessage
     *
     * @return HttpExchangeMessage
     * @see HttpExchangeMessage
     */
    default Mono<HttpExchangeMessage> toExchangeMessage() {
        return Mono
            .zip(
                request()
                    .getBody()
                    .defaultIfEmpty(Unpooled.EMPTY_BUFFER),
                request().multiPart(),
                (body, part) -> new HttpServerExchangeMessage(this, body, part)
            );
    }
}
