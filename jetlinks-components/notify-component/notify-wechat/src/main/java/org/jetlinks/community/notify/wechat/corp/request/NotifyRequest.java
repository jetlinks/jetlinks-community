package org.jetlinks.community.notify.wechat.corp.request;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.community.notify.wechat.corp.response.NotifyResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/90236">企业内部开发>服务端API>消息推送>发送应用消息</a>
 * @since 2.0
 */
@AllArgsConstructor
public class NotifyRequest extends ApiRequest<NotifyResponse> {

    private final Object body;

    @Override
    public Mono<NotifyResponse> execute(WebClient client) {
        return client
            .post()
            .uri("cgi-bin/message/send")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(NotifyResponse.class)
            .doOnNext(ApiResponse::assertSuccess);
    }
}
