package org.jetlinks.community.elastic.search.index.strategies;

import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DirectElasticSearchIndexStrategy extends AbstractElasticSearchIndexStrategy {

    public DirectElasticSearchIndexStrategy(ElasticRestClient client) {
        super("direct", client);
    }

    @Override
    public String getIndexForSave(String index) {
        return index;
    }

    @Override
    public String getIndexForSearch(String index) {
        return index;
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
