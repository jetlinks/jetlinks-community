package org.jetlinks.community.elastic.search.index.strategies;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactor.core.publisher.Mono;

/**
 * 前后缀索引策略支持.
 * <pre>{@code
 *
 *   elasticsearch:
 *      index:
 *          default-strategy: affixes
 *          affixes:
 *            prefix: "" #前缀
 *            suffix: "_test" # 后缀
 *            auto-create: false # 是否创建索引
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 2.2
 */
@Getter
@Setter
@Slf4j
@ConfigurationProperties(prefix = "elasticsearch.index.affixes")
public class AffixesElasticSearchIndexStrategy extends AbstractElasticSearchIndexStrategy {
    //前缀
    private String prefix = "";
    //后缀
    private String suffix = "";
    //是否自动创建索引
    private boolean autoCreate = true;

    public AffixesElasticSearchIndexStrategy(ReactiveElasticsearchClient client,
                                             ElasticSearchIndexProperties properties) {
        super("affixes", client, properties);
    }


    @Override
    public String getIndexForSave(String index) {
        return prefix + index + suffix;
    }

    @Override
    public String getIndexForSearch(String index) {
        return prefix + index + suffix;
    }

    @Override
    @SneakyThrows
    public Mono<ElasticSearchIndexMetadata> putIndex(ElasticSearchIndexMetadata metadata) {

        if (log.isInfoEnabled() && !autoCreate) {
//            CreateIndexRequest request = createIndexRequest(metadata);
//            Object data = ObjectMappers
//                .parseJson(
//                    new RequestCreator() {
//                    }
//                        .createIndexRequest().apply(request).getEntity()
//                        .getContent(), Object.class);
//            log.info("ignore put elasticsearch index [{}] :\n{}", metadata.getIndex(), JSON.toJSONString(data, SerializerFeature.PrettyFormat));
        }

        if (autoCreate) {
            return this
                .doPutIndex(metadata.newIndexName(getIndexForSave(metadata.getIndex())), false)
                .thenReturn(metadata);
        }

        return Mono.just(metadata);
    }

    @Override
    public Mono<ElasticSearchIndexMetadata> loadIndexMetadata(String index) {
        return doLoadIndexMetadata(getIndexForSearch(index));
    }
}
