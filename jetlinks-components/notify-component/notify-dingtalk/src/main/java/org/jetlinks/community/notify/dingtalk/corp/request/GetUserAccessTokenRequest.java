package org.jetlinks.community.notify.dingtalk.corp.request;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.dingtalk.corp.response.AccessTokenResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class GetUserAccessTokenRequest extends ApiRequest<Mono<AccessTokenResponse>> {

    private final String appKey;
    private final String appSecret;
    private final String code;


    @Override
    public Mono<AccessTokenResponse> execute(WebClient client) {

        Map<String, Object> body = new HashMap<>();
        body.put("clientId", appKey);
        body.put("clientSecret", appSecret);
        body.put("code", code);
        body.put("grantType", "authorization_code");


        return client
            .post()
            .uri("https://api.dingtalk.com/v1.0/oauth2/userAccessToken")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(AccessTokenResponse.class);
    }


}
