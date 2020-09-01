package org.jetlinks.community.elastic.search.index.strategies;

import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DirectElasticSearchIndexStrategy extends AbstractElasticSearchIndexStrategy {

    public DirectElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super("direct", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        return wrapIndex(index);
    }

    @Override
    public String getIndexForSearch(String index) {
        return wrapIndex(index);
    }

    @Override
    public Mono<Void> putIndex(ElasticSearchIndexMetadata metadata) {
        return doPutIndex(metadata, false);
    }

    @Override
    public Mono<ElasticSearchIndexMetadata> loadIndexMetadata(String index) {
        return doLoadIndexMetadata(index);
    }

}
