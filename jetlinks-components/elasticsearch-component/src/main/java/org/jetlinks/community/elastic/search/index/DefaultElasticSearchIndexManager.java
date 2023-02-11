package org.jetlinks.community.elastic.search.index;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
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
    private Map<String, String> indexUseStrategy = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private final Map<String, ElasticSearchIndexStrategy> strategies = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private final Map<String, ElasticSearchIndexMetadata> indexMetadataStore = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    public DefaultElasticSearchIndexManager(@Autowired(required = false) List<ElasticSearchIndexStrategy> strategies) {
        if (strategies != null) {
            strategies.forEach(this::registerStrategy);
        }
    }

    @Override
    public Mono<Void> putIndex(ElasticSearchIndexMetadata index) {
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
