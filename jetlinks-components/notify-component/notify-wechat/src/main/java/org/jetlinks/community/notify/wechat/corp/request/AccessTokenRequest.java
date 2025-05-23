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
