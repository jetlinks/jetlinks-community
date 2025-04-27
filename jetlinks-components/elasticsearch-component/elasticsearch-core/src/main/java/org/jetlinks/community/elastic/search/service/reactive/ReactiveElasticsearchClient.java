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
