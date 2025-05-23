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

import org.jetlinks.core.trace.TraceHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public final class TraceExchangeFilterFunction implements ExchangeFilterFunction {

    private static final TraceExchangeFilterFunction INSTANCE = new TraceExchangeFilterFunction();


    public static ExchangeFilterFunction instance() {
        return INSTANCE;
    }

    private TraceExchangeFilterFunction() {
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull ClientRequest request,
                                       @Nonnull ExchangeFunction next) {
        return TraceHolder
            .writeContextTo(ClientRequest.from(request), ClientRequest.Builder::header)
            .flatMap(builder -> next.exchange(builder.build()));
    }

}
