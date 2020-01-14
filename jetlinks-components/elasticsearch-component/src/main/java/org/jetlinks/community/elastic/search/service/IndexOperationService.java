package org.jetlinks.community.elastic.search.service;

import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jetlinks.community.elastic.search.index.mapping.IndexMappingMetadata;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexOperationService {

    Mono<Boolean> indexIsExists(String index);

    Mono<Boolean> init(CreateIndexRequest request);

    Mono<IndexMappingMetadata> getIndexMappingMetadata(String index);
}
