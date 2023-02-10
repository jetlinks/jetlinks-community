package org.jetlinks.community.elastic.search.index.strategies;

import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;

import java.time.LocalDate;

/**
 * 按月对来划分索引策略
 *
 * @author zhouhao
 * @since 1.0
 */
public class TimeByMonthElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {

    public TimeByMonthElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super("time-by-month", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        LocalDate now = LocalDate.now();
        String idx = wrapIndex(index);
        return idx + "_" + now.getYear() + "-" + now.getMonthValue();
    }
}
