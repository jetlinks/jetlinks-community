package org.jetlinks.community.notify.wechat.corp.request;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.wechat.corp.response.AccessTokenResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


/**
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/91039">企业内部开发>服务端API>开发指南>获取access_token</a>
 * @since 2.0
 */
@AllArgsConstructor
public class AccessTokenRequest extends ApiRequest<AccessTokenResponse> {
    private final String corpId;
    private final String corpSecret;

    @Override
    public Mono<AccessTokenResponse> execute(WebClient client) {
        return client
            .get()
            .uri("/cgi-bin/gettoken", uriBuilder -> uriBuilder
                .queryParam("corpid", corpId)
                .queryParam("corpsecret", corpSecret)
                .build())
            .retrieve()
            .bodyToMono(AccessTokenResponse.class)
            .doOnNext(AccessTokenResponse::assertSuccess);
    }
}
