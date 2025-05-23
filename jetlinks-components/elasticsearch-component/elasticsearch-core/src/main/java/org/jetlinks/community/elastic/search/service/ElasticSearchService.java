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
package org.jetlinks.community.elastic.search.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexStrategy;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * ElasticSearch 服务
 *
 * @author zhouhao
 * @see ElasticSearchIndexManager
 * @see ReactiveElasticsearchClient
 * @since 1.0
 */
public interface ElasticSearchService {

    /**
     * 根据索引动态分页查询数据
     *
     * @param index      索引
     * @param queryParam 查询参数
     * @param mapper     结果转换器
     * @param <T>        结果类型
     * @return 分页查询结果
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    default <T> Mono<PagerResult<T>> queryPager(String index,
                                                QueryParam queryParam,
                                                Function<Map<String, Object>, T> mapper) {
        return queryPager(new String[]{index}, queryParam, mapper);
    }

    /**
     * 分页查询多个索引数据
     *
     * @param index      索引列表
     * @param queryParam 查询条件
     * @param mapper     结果转换器
     * @param <T>        结果类型
     * @return 分页查询结果
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    <T> Mono<PagerResult<T>> queryPager(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper);

    /**
     * 查询数据
     *
     * @param index      索引
     * @param queryParam 查询条件
     * @param mapper     结果转换器
     * @param <T>        结果类型
     * @return 查询结果
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    <T> Flux<T> query(String index, QueryParam queryParam, Function<Map<String, Object>, T> mapper);

    /**
     * 查询多个索引数据
     *
     * @param index      索引
     * @param queryParam 查询条件
     * @param mapper     结果转换器
     * @param <T>        结果类型
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    <T> Flux<T> query(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper);

    /**
     * 根据多个条件,执行多次查询并一次性返回结果
     *
     * @param index      索引
     * @param queryParam 查询条件
     * @param mapper     结果转换器
     * @param <T>        结果类型
     * @return 查询结果
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    default <T> Flux<T> multiQuery(String index, Collection<QueryParam> queryParam, Function<Map<String, Object>, T> mapper) {
        return multiQuery(new String[]{index}, queryParam, mapper);
    }

    /**
     * 根据多个条件,执行多次查询并一次性返回结果
     *
     * @param index      索引
     * @param queryParam 查询条件
     * @param mapper     结果转换器
     * @param <T>        结果类型
     * @return 查询结果
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    <T> Flux<T> multiQuery(String[] index, Collection<QueryParam> queryParam, Function<Map<String, Object>, T> mapper);

    /**
     * 查询数据总数
     *
     * @param index      索引
     * @param queryParam 查询条件
     * @return 查询结果
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    default Mono<Long> count(String index, QueryParam queryParam) {
        return count(new String[]{index}, queryParam);
    }

    /**
     * 查询数据总数
     *
     * @param index      索引
     * @param queryParam 查询条件
     * @return 查询结果
     * @see ElasticSearchIndexStrategy
     * @see ElasticSearchIndexStrategy#getIndexForSearch(String)
     */
    Mono<Long> count(String[] index, QueryParam queryParam);

    /**
     * 按查询条件删除数据
     *
     * @param index      索引
     * @param queryParam 查询条件
     * @return 查询结果
     */
    Mono<Long> delete(String index, QueryParam queryParam);

    /**
     * 提交一个索引记录,此操作不会立即存储数据,将会进行本地缓冲,满足一定条件后批量写入es.
     *
     * @param index   索引
     * @param payload 记录值
     * @param <T>     类型
     * @return void
     */
    <T> Mono<Void> commit(String index, T payload);

    /**
     * 提交多个索引记录,此操作不会立即存储数据,将会进行本地缓冲,满足一定条件后批量写入es.
     *
     * @param index   索引
     * @param payload 记录值
     * @param <T>     类型
     * @return void
     */
    <T> Mono<Void> commit(String index, Collection<T> payload);

    /**
     * 提交多个索引记录,此操作不会立即存储数据,将会进行本地缓冲,满足一定条件后批量写入es.
     *
     * @param index   索引
     * @param payload 记录值
     * @param <T>     类型
     * @return void
     */
    <T> Mono<Void> commit(String index, Publisher<T> payload);

    /**
     * 立即保存索引记录
     *
     * @param index   索引
     * @param payload 记录值
     * @param <T>     类型
     * @return void
     */
    <T> Mono<Void> save(String index, T payload);

    /**
     * 立即保存多个索引记录
     *
     * @param index   索引
     * @param payload 记录值
     * @param <T>     类型
     * @return void
     */
    <T> Mono<Void> save(String index, Collection<T> payload);

    /**
     * 立即保存多个记录
     *
     * @param index   索引
     * @param payload 记录值
     * @param <T>     类型
     * @return void
     */
    <T> Mono<Void> save(String index, Publisher<T> payload);

    /*====== 查询并转换为指定的类型 ======*/

    default <T> Flux<T> query(String index, QueryParam queryParam, Class<T> type) {
        return query(index, queryParam, map -> FastBeanCopier.copy(map, type));
    }

    default <T> Mono<PagerResult<T>> queryPager(String index, QueryParam queryParam, Class<T> type) {
        return queryPager(index, queryParam, map -> FastBeanCopier.copy(map, type));
    }

    default <T> Mono<PagerResult<T>> queryPager(ElasticIndex index, QueryParam queryParam, Class<T> type) {
        return queryPager(index.getIndex(), queryParam, type);
    }

    default <T> Mono<PagerResult<T>> queryPager(ElasticIndex index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return queryPager(index.getIndex(), queryParam, mapper);
    }

    /*====== 使用索引定义执行响应到操作 ======*/
    default <T> Flux<T> query(ElasticIndex index, QueryParam queryParam, Class<T> type) {
        return query(index.getIndex(), queryParam, type);
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
