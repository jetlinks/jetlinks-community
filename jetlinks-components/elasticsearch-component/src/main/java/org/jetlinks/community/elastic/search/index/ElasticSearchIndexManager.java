package org.jetlinks.community.elastic.search.index;

import reactor.core.publisher.Mono;

public interface ElasticSearchIndexManager {

    Mono<Void> putIndex(ElasticSearchIndexMetadata index);

    Mono<ElasticSearchIndexMetadata> getIndexMetadata(String index);

    Mono<ElasticSearchIndexStrategy> getIndexStrategy(String index);

}
