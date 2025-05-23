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

import java.util.HashMap;
import java.util.Map;

/**
 * @author bestfeng
 *
 * 根据unionid获取用户userid
 *
 * @see <a href="https://open.dingtalk.com/document/orgapp/query-a-user-by-the-union-id">根据unionid获取用户userid</a>
 */
@AllArgsConstructor
public class GetUserIdByUnionIdRequest extends ApiRequest<Mono<GetUserIdByUnionIdRequest.UserUnionInfoResponse>> {

    private final String unionId;

    @Override
    public Mono<UserUnionInfoResponse> execute(WebClient client) {
        return doRequest(client);
    }

    private Mono<UserUnionInfoResponse> doRequest(WebClient client) {

        Map<String, Object> body = new HashMap<>();
        body.put("unionid", unionId);
        return client
            .post()
            .uri("/topapi/user/getbyunionid")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(UserUnionInfoResponse.class)
            .doOnNext(rep -> {
                if (rep.getResult() == null) {
                    throw new BusinessException(rep.getErrorMessage());
                }
            });
    }

    @Getter
    @Setter
    public static class UserUnionInfoResponse extends ApiResponse {

        @JsonProperty
        @JsonAlias("result")
        private Result result;

        @Getter
        @Setter
        public static class Result {

            @JsonProperty
            @JsonAlias("contact_type")
            private int contactType;

            @JsonProperty
            @JsonAlias("userid")
            private String userId;
        }

    }
}
