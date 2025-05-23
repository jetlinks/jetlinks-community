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

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.jetlinks.core.cache.Caches;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@ConfigurationProperties(prefix = "elasticsearch.index")
public class DefaultElasticSearchIndexManager implements ElasticSearchIndexManager {

    @Getter
    @Setter
    @Generated
    private String defaultStrategy = "direct";

    @Getter
    @Setter
    @Generated
    //是否自动创建索引
    private boolean autoCreate = true;

    @Getter
    @Setter
    @Generated
    private Map<String, String> indexUseStrategy = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private final Map<String, ElasticSearchIndexStrategy> strategies = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private final Map<String, ElasticSearchIndexMetadata> indexMetadataStore = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    public DefaultElasticSearchIndexManager(@Autowired(required = false) List<ElasticSearchIndexStrategy> strategies,
                                            @Autowired(required = false) List<ElasticSearchIndexCustomizer> customizers) {
        if (strategies != null) {
            strategies.forEach(this::registerStrategy);
        }
        if (customizers != null) {
            customizers.forEach(customizer -> customizer.custom(this));
        }
    }

    @Override
    public Mono<Void> putIndex(ElasticSearchIndexMetadata index) {
        //禁用索引创建
        if (!autoCreate) {
            indexMetadataStore.put(index.getIndex(), index);
            return Mono.empty();
        }
        return this.getIndexStrategy(index.getIndex())
                   .flatMap(strategy -> strategy.putIndex(index))
                   .doOnNext(idx -> indexMetadataStore.put(idx.getIndex(), idx))
                   .then();
    }

    @Override
    public Mono<ElasticSearchIndexMetadata> getIndexMetadata(String index) {
        return Mono.justOrEmpty(indexMetadataStore.get(index))
                   .switchIfEmpty(Mono.defer(() -> doLoadMetaData(index)
                       .doOnNext(metadata -> indexMetadataStore.put(metadata.getIndex(), metadata))));
    }

    protected Mono<ElasticSearchIndexMetadata> doLoadMetaData(String index) {
        return getIndexStrategy(index)
            .flatMap(strategy -> strategy.loadIndexMetadata(index));
    }

    @Override
    public Mono<ElasticSearchIndexStrategy> getIndexStrategy(String index) {
        return Mono.justOrEmpty(strategies.get(indexUseStrategy.getOrDefault(index.toLowerCase(), defaultStrategy)))
                   .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("[" + index + "] 不支持任何索引策略")));
    }

    @Override
    public void useStrategy(String index, String strategy) {
        indexUseStrategy.put(index, strategy);
    }

    public void registerStrategy(ElasticSearchIndexStrategy strategy) {
        strategies.put(strategy.getId(), strategy);
    }

}
