package org.jetlinks.community.elastic.search.index.strategies;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.utils.ReactorActionListener;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TemplateElasticSearchIndexStrategy extends AbstractElasticSearchIndexStrategy {

    public TemplateElasticSearchIndexStrategy(String id, ElasticRestClient client, ElasticSearchIndexProperties properties) {
        super(id, client,properties);
    }

    protected String getTemplate(String index) {
        return wrapIndex(index).concat("_template");
    }

    protected String getAlias(String index) {
        return wrapIndex(index).concat("_alias");
    }

    protected List<String> getIndexPatterns(String index) {
        return Collections.singletonList(wrapIndex(index).concat("*"));
    }

    @Override
    public abstract String getIndexForSave(String index);

    @Override
    public String getIndexForSearch(String index) {
        return getAlias(index);
    }

    @Override
    public Mono<Void> putIndex(ElasticSearchIndexMetadata metadata) {
        return ReactorActionListener
            .<AcknowledgedResponse>mono(listener -> client.getWriteClient()
                .indices()//修改索引模版
                .putTemplateAsync(createIndexTemplateRequest(metadata), RequestOptions.DEFAULT, listener))
            //修改当前索引
            .then(doPutIndex(metadata.newIndexName(getIndexForSave(metadata.getIndex())), true));
    }


    protected PutIndexTemplateRequest createIndexTemplateRequest(ElasticSearchIndexMetadata metadata) {
        String index = wrapIndex(metadata.getIndex());
        PutIndexTemplateRequest request = new PutIndexTemplateRequest(getTemplate(index));
        request.alias(new Alias(getAlias(index)));
        request.settings(properties.toSettings());
        Map<String, Object> mappingConfig = new HashMap<>();
        mappingConfig.put("properties", createElasticProperties(metadata.getProperties()));
        mappingConfig.put("dynamic_templates", createDynamicTemplates());
        request.mapping(mappingConfig);
        request.patterns(getIndexPatterns(index));
        return request;
    }


    @Override
    public Mono<ElasticSearchIndexMetadata> loadIndexMetadata(String index) {

        return ReactorActionListener
            .<GetIndexTemplatesResponse>mono(listener -> client.getQueryClient()
                .indices()
                .getIndexTemplateAsync(new GetIndexTemplatesRequest(getTemplate(index)), RequestOptions.DEFAULT, listener))
            .filter(resp -> resp.getIndexTemplates().size() > 0)
            .flatMap(resp -> Mono.justOrEmpty(convertMetadata(index, resp.getIndexTemplates().get(0).mappings())));
    }
}
