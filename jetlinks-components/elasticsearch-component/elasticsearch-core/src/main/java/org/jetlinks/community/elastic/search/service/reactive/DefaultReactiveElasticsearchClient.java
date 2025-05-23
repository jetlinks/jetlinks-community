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
package org.jetlinks.community.elastic.search.service.reactive;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.Version;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.TraceHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class DefaultReactiveElasticsearchClient implements ReactiveElasticsearchClient {
    private final static SharedPathString TRACE_SPAN_NAME_EXECUTE =
        SharedPathString.of("/DefaultReactiveElasticsearchClient/execute");
    private final static SharedPathString TRACE_SPAN_NAME_EXECUTE_ASYNC =
        SharedPathString.of("/DefaultReactiveElasticsearchClient/executeAsync");

    private final ElasticsearchClient client;
    private final ElasticsearchAsyncClient asyncClient;

    private Version serverVersion;

    public DefaultReactiveElasticsearchClient(ElasticsearchClient client) {
        this.client = client;
        this.asyncClient = new ElasticsearchAsyncClient(client._transport(), client._transportOptions());
    }

    @Override
    @SneakyThrows
    public Version serverVersion() {
        if (serverVersion != null) {
            return serverVersion;
        }
        synchronized (this) {
            return serverVersion = Version
                .parse(client.info()
                             .version()
                             .number());
        }

    }

    @Override
    public <T> Mono<T> executeAsync(ElasticsearchAsyncClientCallback<T> callback) {
        return Mono
            .defer(() -> {
                try {
                    return Mono
                        .fromCompletionStage(callback.execute(asyncClient));
                } catch (Exception e) {
                    return Mono.error(e);
                }
            })
            .as(MonoTracer.create(TRACE_SPAN_NAME_EXECUTE_ASYNC));
    }

    @Override
    public <T> Mono<T> execute(ElasticsearchClientCallback<T> callback) {
        return Mono
            .fromCallable(() -> callback.execute(client))
            .as(MonoTracer.create(TRACE_SPAN_NAME_EXECUTE))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
