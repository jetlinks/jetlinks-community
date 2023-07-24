package org.jetlinks.community.notify.wechat.corp.request;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetUserInfoResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 获取用户身份请求.
 *
 * @author zhangji 2023/6/8
 * @see <a href="https://developer.work.weixin.qq.com/document/path/91023">获取访问用户身份</a>
 * @since 2.1
 */
@AllArgsConstructor
public class GetUserInfoRequest extends ApiRequest<GetUserInfoResponse> {

    private final String code;

    @Override
    public Mono<GetUserInfoResponse> execute(WebClient client) {
        return client
            .get()
            .uri("/cgi-bin/auth/getuserinfo", builder -> builder
                .queryParam("code", code)
                .build())
            .retrieve()
            .bodyToMono(GetUserInfoResponse.class)
            .doOnNext(ApiResponse::assertSuccess);
    }
}
