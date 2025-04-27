package org.jetlinks.community.elastic.search.index.strategies;

import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 按天来划分索引策略
 *
 * @author zhouhao
 * @since 2.2
 */
public class TimeByDayElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    public TimeByDayElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super("time-by-day", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        LocalDate now = LocalDate.now(CLOCK);
        String idx = wrapIndex(index);
        return idx + "_" + now.getYear() + "-" + now.getMonthValue() + "-" + now.getDayOfMonth();
    }
}
