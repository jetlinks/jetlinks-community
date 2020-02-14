package org.jetlinks.community.elastic.search.service;

import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexTemplateOperationService {

    Mono<Boolean> indexTemplateIsExists(String index);

    Mono<Boolean> putTemplate(PutIndexTemplateRequest request);
}
