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
package org.jetlinks.community.elastic.search.index;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ElasticSearch 索引管理器,用于统一管理,维护索引信息.
 *
 * @author zhouhao
 * @since 1.0
 */
public interface ElasticSearchIndexManager {

    /**
     * 更新索引
     *
     * @param index 索引元数据
     * @return 更新结果
     */
    Mono<Void> putIndex(ElasticSearchIndexMetadata index);

    /**
     * 获取索引元数据
     *
     * @param index 索引名称
     * @return 索引元数据
     */
    Mono<ElasticSearchIndexMetadata> getIndexMetadata(String index);

    /**
     * 获取多个所有元数据
     *
     * @param index 索引名称
     * @return 索引元数据
     */
    default Flux<ElasticSearchIndexMetadata> getIndexesMetadata(String... index) {
        return Flux
            .fromArray(index)
            .flatMap(this::getIndexMetadata);
    }

    /**
     * 获取索引策略
     *
     * @param index 索引名称
     * @return 索引策略
     * @see ElasticSearchIndexStrategy
     */
    Mono<ElasticSearchIndexStrategy> getIndexStrategy(String index);

    /**
     * 获取多个索引的策略
     *
     * @param index 索引列表
     * @return 索引策略
     */
    default Flux<ElasticSearchIndexStrategy> getIndexesStrategy(String... index) {
        return Flux
            .fromArray(index)
            .flatMap(this::getIndexStrategy);
    }

    /**
     * 设置索引策略
     *
     * @param index    索引策略
     * @param strategy 策略标识
     */
    void useStrategy(String index, String strategy);

    /**
     * 注册索引策略
     *
     * @param strategy 策略
     */
    void registerStrategy(ElasticSearchIndexStrategy strategy);

}
