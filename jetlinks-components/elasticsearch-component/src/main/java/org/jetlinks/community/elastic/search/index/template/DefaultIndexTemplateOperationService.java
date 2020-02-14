package org.jetlinks.community.elastic.search.index.template;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.service.IndexTemplateOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class DefaultIndexTemplateOperationService implements IndexTemplateOperationService {

    private final ElasticRestClient restClient;

    @Autowired
    public DefaultIndexTemplateOperationService(ElasticRestClient restClient) {
        this.restClient = restClient;
    }


    @Override
    public Mono<Boolean> indexTemplateIsExists(String name) {
        return Mono.create(sink -> {
            IndexTemplatesExistRequest request = new IndexTemplatesExistRequest(name);
            restClient.getQueryClient().indices().existsTemplateAsync(request, RequestOptions.DEFAULT, new ActionListener<Boolean>() {
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
    public Mono<Boolean> putTemplate(PutIndexTemplateRequest request) {
        return indexTemplateIsExists(request.name())
            .filter(bool -> !bool)
            .flatMap(b -> Mono.create(sink -> {
                restClient.getQueryClient().indices().putTemplateAsync(request, RequestOptions.DEFAULT, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse acknowledgedResponse) {
                        sink.success(acknowledgedResponse.isAcknowledged());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        sink.error(e);
                    }
                });
            }));
    }
}
