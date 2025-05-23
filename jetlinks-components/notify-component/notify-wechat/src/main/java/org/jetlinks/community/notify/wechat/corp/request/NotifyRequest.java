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
import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.community.notify.wechat.corp.response.NotifyResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/90236">企业内部开发>服务端API>消息推送>发送应用消息</a>
 * @since 2.0
 */
@AllArgsConstructor
public class NotifyRequest extends ApiRequest<NotifyResponse> {

    private final Object body;

    @Override
    public Mono<NotifyResponse> execute(WebClient client) {
        return client
            .post()
            .uri("cgi-bin/message/send")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(NotifyResponse.class)
            .doOnNext(ApiResponse::assertSuccess);
    }
}
