/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
