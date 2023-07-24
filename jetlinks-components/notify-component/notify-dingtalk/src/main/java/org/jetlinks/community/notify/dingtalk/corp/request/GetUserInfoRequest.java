package org.jetlinks.community.notify.dingtalk.corp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.notify.dingtalk.corp.response.ApiResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author bestfeng
 * <p>
 * 通过oauth2授权获取用户信息
 * @see <a href="https://open.dingtalk.com/document/orgapp/obtain-identity-credentials">获取登录用户的访问凭证</a>
 * @see <a href="https://open.dingtalk.com/document/orgapp/dingtalk-retrieve-user-information">获取用户通讯录个人信息</a>
 */
@AllArgsConstructor
public class GetUserInfoRequest extends ApiRequest<Mono<GetUserInfoRequest.UserInfoResponse>> {

    private final String accessToken;

    @Override
    public Mono<UserInfoResponse> execute(WebClient client) {
        return doRequest(client);
    }

    private Mono<UserInfoResponse> doRequest(WebClient client) {
        return client
            .get()
            .uri("https://api.dingtalk.com/v1.0/contact/users/{unionId}", "me")
            .header("x-acs-dingtalk-access-token", accessToken)
            .retrieve()
            .bodyToMono(UserInfoResponse.class)
            .doOnNext(rep -> {
                if (rep.getUnionId() == null) {
                    throw new BusinessException(rep.getErrorMessage());
                }
            });

    }


    @Getter
    @Setter
    public static class UserInfoResponse extends ApiResponse {

        @JsonAlias("unionid")
        @JsonProperty
        private String unionId;

        @JsonAlias("avatar_url")
        @JsonProperty
        private String avatarUrl;

        @JsonAlias("nick")
        @JsonProperty
        private String nick;

    }
}
