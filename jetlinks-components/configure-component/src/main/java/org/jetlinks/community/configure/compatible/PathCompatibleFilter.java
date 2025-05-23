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
package org.jetlinks.community.configure.compatible;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 兼容处理路径结尾为/的请求
 *
 * @since 2.10
 */
public class PathCompatibleFilter implements WebFilter {
    @Override
    @Nonnull
    public Mono<Void> filter(ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        if (!path.equals("/") && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
            return chain.filter(
                exchange
                    .mutate()
                    .request(exchange
                                 .getRequest()
                                 .mutate()
                                 .path(path)
                                 .build())
                    .build());
        }
        return chain.filter(exchange);
    }
}
