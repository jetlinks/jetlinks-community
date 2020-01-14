package org.jetlinks.community.elastic.search.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ElasticSearchService {


    <T> Mono<PagerResult<T>> queryPager(ElasticIndex index, QueryParam queryParam, Class<T> type);

    <T> Flux<T> query(ElasticIndex index, QueryParam queryParam, Class<T> type);

    Mono<Long> count(ElasticIndex index, QueryParam queryParam);

    <T> Mono<Void> commit(ElasticIndex index, T payload);

    <T> Mono<Void> commit(ElasticIndex index, Collection<T> payload);

}
