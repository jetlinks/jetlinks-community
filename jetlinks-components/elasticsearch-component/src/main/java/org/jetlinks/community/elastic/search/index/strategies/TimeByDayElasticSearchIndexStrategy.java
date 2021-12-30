package org.jetlinks.community.elastic.search.index.strategies;

import org.hswebframework.utils.time.DateFormatter;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;

/**
 * 按日期来划分索引策略
 *
 * @author caizz
 * @since 1.0
 */
@Component
public class TimeByDayElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {

    public TimeByDayElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super("time-by-day", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        LocalDate now = LocalDate.now();
        String idx = wrapIndex(index);
        return idx + "_" + now.getYear()
            + "-" + (now.getMonthValue() < 10 ? "0" : "") + now.getMonthValue()
            + "-" + (now.getDayOfMonth() < 10 ? "0" : "") + now.getDayOfMonth();
    }
}
