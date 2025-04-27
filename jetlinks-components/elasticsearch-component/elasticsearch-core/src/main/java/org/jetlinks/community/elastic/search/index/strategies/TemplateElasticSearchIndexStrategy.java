package org.jetlinks.community.elastic.search.index.strategies;

import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.TemplateMapping;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.elastic.search.ElasticSearchSupport;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

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
        if (properties.isUseAliasSearch()) {
            return getAlias(index);
        } else {
            return wrapIndex(index).concat("*");
        }
    }

    @Override
    public Mono<ElasticSearchIndexMetadata> putIndex(ElasticSearchIndexMetadata metadata) {
        String saveIndex = getIndexForSave(metadata.getIndex());

        return client
            .execute(c -> c
                .indices()
                .putIndexTemplate(request -> createIndexTemplateRequest(request, metadata)))
            .then(doPutIndex(metadata.newIndexName(saveIndex), true)
                      //忽略修改索引错误
                      .onErrorResume(err -> {
                          log.warn("Update Index[{}] Mapping error", saveIndex, err);
                          return Mono.empty();
                      }))
            .thenReturn(metadata.newIndexName(wrapIndex(metadata.getIndex())));
    }

    protected PutIndexTemplateRequest.Builder createIndexTemplateRequest(PutIndexTemplateRequest.Builder builder,
                                                                         ElasticSearchIndexMetadata metadata) {
        String index = wrapIndex(metadata.getIndex());
        builder.name(getTemplate(index));

        builder.indexPatterns(getIndexPatterns(index));

        // 7.x不支持此设置
        if (ElasticSearchSupport.current().is8x()) {
            builder.allowAutoCreate(true);
        }

        builder.template(template -> {
            template.aliases(getAlias(index), a -> a);
            template.mappings(mapping -> {
                mapping.dynamicTemplates(createDynamicTemplates());
                mapping.properties(createElasticProperties(metadata.getProperties()));
                mapping.source(s -> s.enabled(true));
                return mapping;
            });
            return template;
        });
        return builder;
    }


    @Override
    public Mono<ElasticSearchIndexMetadata> loadIndexMetadata(String index) {
        String name = getTemplate(index);
        return client.execute(t -> {
            TemplateMapping mapping = ElasticSearchSupport
                .current()
                .getTemplateMapping(
                    t.indices()
                     .getTemplate(request -> request.name(getTemplate(index))),
                    name
                );
            return mapping == null ? null : convertMetadata(index, mapping.mappings());
        });
    }
}
