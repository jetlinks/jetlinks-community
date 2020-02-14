package org.jetlinks.community.elastic.search.service;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexAliasOperationService {

    Mono<Boolean> indexAliasIsExists(String index);

    Mono<Boolean> AddAlias(IndicesAliasesRequest request);
}
