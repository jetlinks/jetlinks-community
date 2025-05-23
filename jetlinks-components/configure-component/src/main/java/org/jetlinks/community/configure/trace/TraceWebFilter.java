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
package org.jetlinks.community.configure.trace;

import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class TraceWebFilter implements WebFilter, Ordered {
    @SuppressWarnings("all")
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             WebFilterChain chain) {
        //    /http/method/path
        String spanName = "/http/"+exchange.getRequest().getMethod().name()  + exchange.getRequest().getPath().value();

        ServerHttpRequest.Builder requestCopy = exchange
            .getRequest()
            .mutate();

        return TraceHolder
            //将追踪信息返回到响应头
            .writeContextTo(exchange.getResponse().getHeaders(), HttpHeaderTraceWriter.INSTANCE)
            //传递到下游请求头中
            .then(TraceHolder.writeContextTo(requestCopy, HttpServerHeaderTraceWriter.INSTANCE))
            //do filter
            .then(Mono.defer(() -> chain.filter(exchange.mutate().request(requestCopy.build()).build())))
            //创建跟踪信息
            .as(MonoTracer.create(spanName))
            //从请求头中追加上级跟踪信息
            .contextWrite(ctx -> {
                return TraceHolder.readToContext(
                    ctx,
                    exchange.getRequest().getHeaders(),
                    HttpHeadersGetter.INSTANCE);
            });
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 100;
    }
}
