package org.jetlinks.community.elastic.search.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public interface ElasticSearchService {

    default <T> Mono<PagerResult<T>> queryPager(String index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return queryPager(new String[]{index}, queryParam, mapper);
    }

    <T> Mono<PagerResult<T>> queryPager(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper);

    <T> Flux<T> query(String index, QueryParam queryParam, Function<Map<String, Object>, T> mapper);

    <T> Flux<T> query(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper);

    default <T> Flux<T> multiQuery(String index, Collection<QueryParam> queryParam, Function<Map<String, Object>, T> mapper) {
        return multiQuery(new String[]{index}, queryParam, mapper);
    }

    <T> Flux<T> multiQuery(String[] index, Collection<QueryParam> queryParam, Function<Map<String, Object>, T> mapper);

    default Mono<Long> count(String index, QueryParam queryParam) {
        return count(new String[]{index}, queryParam);
    }

    Mono<Long> count(String[] index, QueryParam queryParam);


    Mono<Long> delete(String index, QueryParam queryParam);

    <T> Mono<Void> commit(String index, T payload);

    <T> Mono<Void> commit(String index, Collection<T> payload);

    <T> Mono<Void> commit(String index, Publisher<T> data);

    <T> Mono<Void> save(String index, T payload);

    <T> Mono<Void> save(String index, Collection<T> payload);

    <T> Mono<Void> save(String index, Publisher<T> data);

    default <T> Flux<T> query(String index, QueryParam queryParam, Class<T> type) {
        return query(index, queryParam, map -> FastBeanCopier.copy(map, type));
    }

    default <T> Mono<PagerResult<T>> queryPager(String index, QueryParam queryParam, Class<T> type) {
        return queryPager(index, queryParam, map -> FastBeanCopier.copy(map, type));
    }

    default <T> Mono<PagerResult<T>> queryPager(ElasticIndex index, QueryParam queryParam, Class<T> type) {
        return queryPager(index.getIndex(), queryParam, map -> FastBeanCopier.copy(map, type));
    }

    default <T> Mono<PagerResult<T>> queryPager(ElasticIndex index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return queryPager(index.getIndex(), queryParam, mapper);
    }

    default <T> Flux<T> query(ElasticIndex index, QueryParam queryParam, Class<T> type) {
        return query(index.getIndex(), queryParam, map -> FastBeanCopier.copy(map, type));
    }

    default <T> Flux<T> query(ElasticIndex index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return this.query(index.getIndex(), queryParam, mapper);
    }

    default <T> Mono<Long> count(ElasticIndex index, QueryParam data) {
        return this.count(index.getIndex(), data);
    }

    default <T> Mono<Void> commit(ElasticIndex index, T data) {
        return this.commit(index.getIndex(), data);
    }

    default <T> Mono<Void> commit(ElasticIndex index, Collection<T> data) {
        return this.commit(index.getIndex(), data);
    }

    default <T> Mono<Void> commit(ElasticIndex index, Publisher<T> data) {
        return this.commit(index.getIndex(), data);
    }

}
