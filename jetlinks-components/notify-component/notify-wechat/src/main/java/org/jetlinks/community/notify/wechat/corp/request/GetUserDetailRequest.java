package org.jetlinks.community.notify.wechat.corp.request;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetUserDetailResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 获取用户信息请求.
 *
 * @author zhangji 2023/6/8
 * @see <a href="https://developer.work.weixin.qq.com/document/path/90196">获取用户</a>
 * @since 2.1
 */
@AllArgsConstructor
public class GetUserDetailRequest extends ApiRequest<GetUserDetailResponse> {

    private final String userId;

    @Override
    public Mono<GetUserDetailResponse> execute(WebClient client) {
        return client
            .get()
            .uri("/cgi-bin/user/get", builder -> builder
                .queryParam("userid", userId)
                .build())
            .retrieve()
            .bodyToMono(GetUserDetailResponse.class)
            .doOnNext(ApiResponse::assertSuccess);
    }
}
