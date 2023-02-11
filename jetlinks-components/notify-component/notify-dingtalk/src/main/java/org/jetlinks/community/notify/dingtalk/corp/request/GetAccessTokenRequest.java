package org.jetlinks.community.notify.dingtalk.corp.request;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.dingtalk.corp.response.AccessTokenResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class GetAccessTokenRequest extends ApiRequest<Mono<AccessTokenResponse>> {

    private final String appKey;
    private final String appSecret;

    @Override
    public Mono<AccessTokenResponse> execute(WebClient client) {
        return client
            .get()
            .uri("gettoken", uri -> uri
                .queryParam("appkey", appKey)
                .queryParam("appsecret", appSecret)
                .build())
            .retrieve()
            .bodyToMono(AccessTokenResponse.class);
    }


}
