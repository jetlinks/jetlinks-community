package org.jetlinks.community.elastic.search.index.strategies;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class TemplateElasticSearchIndexStrategy extends AbstractElasticSearchIndexStrategy {

    public TemplateElasticSearchIndexStrategy(String id, ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super(id, client, properties);
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
    public Mono<ElasticSearchIndexMetadata> putIndex(ElasticSearchIndexMetadata metadata) {
        String saveIndex = getIndexForSave(metadata.getIndex());
        return client
            .putTemplate(createIndexTemplateRequest(metadata))
            //修改当前索引
            .then(doPutIndex(metadata.newIndexName(saveIndex), true)
                      //忽略修改索引错误
                      .onErrorResume(err -> {
                          log.warn("Update Index[{}] Mapping error", saveIndex, err);
                          return Mono.empty();
                      }))
            .thenReturn(metadata.newIndexName(wrapIndex(metadata.getIndex())));
    }

    protected PutIndexTemplateRequest createIndexTemplateRequest(ElasticSearchIndexMetadata metadata) {
        String index = wrapIndex(metadata.getIndex());
        PutIndexTemplateRequest request = new PutIndexTemplateRequest(getTemplate(index));
        request.alias(new Alias(getAlias(index)));
        request.settings(properties.toSettings());
        Map<String, Object> mappingConfig = new HashMap<>();
        mappingConfig.put("properties", createElasticProperties(metadata.getProperties()));
        mappingConfig.put("dynamic_templates", createDynamicTemplates());
        mappingConfig.put("_source", Collections.singletonMap("enabled", true));
        if (client.serverVersion().after(Version.V_7_0_0)) {
            request.mapping(mappingConfig);
        } else {
            request.mapping(Collections.singletonMap("_doc", mappingConfig));
        }
        request.patterns(getIndexPatterns(index));
        return request;
    }


    @Override
    public Mono<ElasticSearchIndexMetadata> loadIndexMetadata(String index) {
        return client.getTemplate(new GetIndexTemplatesRequest(getTemplate(index)))
                     .filter(resp -> CollectionUtils.isNotEmpty(resp.getIndexTemplates()))
                     .flatMap(resp -> Mono.justOrEmpty(convertMetadata(index, resp
                         .getIndexTemplates()
                         .get(0)
                         .mappings())));
    }
}
