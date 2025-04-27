package org.jetlinks.community.elastic.search.index.strategies;

import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import reactor.core.publisher.Mono;

public class DirectElasticSearchIndexStrategy extends AbstractElasticSearchIndexStrategy {

    public static String ID = "direct";

    public DirectElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super(ID, client, properties);
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
    public Mono<ElasticSearchIndexMetadata> putIndex(ElasticSearchIndexMetadata metadata) {
        ElasticSearchIndexMetadata index = metadata.newIndexName(wrapIndex(metadata.getIndex()));
        return doPutIndex(index, false)
            .thenReturn(index);
    }

    @Override
    public Mono<ElasticSearchIndexMetadata> loadIndexMetadata(String index) {
        return doLoadIndexMetadata(index);
    }

}
