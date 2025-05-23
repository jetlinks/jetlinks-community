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
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ReactiveElasticsearchClient {

    Version serverVersion();

    <T> Mono<T> execute(ElasticsearchClientCallback<T> callback);

    <T> Mono<T> executeAsync(ElasticsearchAsyncClientCallback<T> callback);


    interface ElasticsearchAsyncClientCallback<T> extends Function<ElasticsearchAsyncClient, CompletableFuture<T>> {

        @Override
        @SneakyThrows
        default CompletableFuture<T> apply(ElasticsearchAsyncClient client) {
            return execute(client);
        }

        CompletableFuture<T> execute(ElasticsearchAsyncClient client) throws Exception;
    }

    interface ElasticsearchClientCallback<T> extends Function<ElasticsearchClient, T> {

        @Override
        @SneakyThrows
        default T apply(ElasticsearchClient client) {
            return execute(client);
        }

        T execute(ElasticsearchClient client) throws Exception;
    }
}
