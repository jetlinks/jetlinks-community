package org.jetlinks.community.elastic.search.index.alias;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.service.IndexAliasOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class DefaultIndexAliasOperationService implements IndexAliasOperationService {

    private final ElasticRestClient restClient;

    @Autowired
    public DefaultIndexAliasOperationService(ElasticRestClient restClient) {
        this.restClient = restClient;
    }


    @Override
    public Mono<Boolean> indexAliasIsExists(String alias) {
        return Mono.create(sink -> {
            GetAliasesRequest request = new GetAliasesRequest(alias);
            restClient.getQueryClient().indices().existsAliasAsync(request, RequestOptions.DEFAULT, new ActionListener<Boolean>() {
                @Override
                public void onResponse(Boolean aBoolean) {
                    sink.success(aBoolean);
                }

                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    @Override
    public Mono<Boolean> AddAlias(IndicesAliasesRequest request) {
        return Mono.create(sink -> {
            restClient.getQueryClient().indices().updateAliasesAsync(request, RequestOptions.DEFAULT, new ActionListener<AcknowledgedResponse>() {
                @Override
                public void onResponse(AcknowledgedResponse acknowledgedResponse) {
                    sink.success(acknowledgedResponse.isAcknowledged());
                }

                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });

    }
}
