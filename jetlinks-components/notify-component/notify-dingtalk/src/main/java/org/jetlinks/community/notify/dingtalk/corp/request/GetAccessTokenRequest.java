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
