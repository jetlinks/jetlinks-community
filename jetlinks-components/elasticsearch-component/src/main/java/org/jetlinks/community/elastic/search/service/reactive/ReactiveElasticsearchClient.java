package org.jetlinks.community.elastic.search.service.reactive;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import reactor.core.publisher.Mono;

public interface ReactiveElasticsearchClient extends
    org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
    , org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices {

    Mono<SearchResponse> searchForPage(SearchRequest request);

    Mono<MultiSearchResponse> multiSearch(MultiSearchRequest request);

    Mono<GetMappingsResponse> getMapping(GetMappingsRequest request);

    Mono<GetIndexTemplatesResponse> getTemplate(GetIndexTemplatesRequest request);

    Mono<AcknowledgedResponse> updateTemplate(PutIndexTemplateRequest request);

    Version serverVersion();
}
