package org.jetlinks.community.elastic.search.index.strategies;

import org.hswebframework.utils.time.DateFormatter;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 按日对来划分索引策略
 *
 * @author caizz
 * @since 1.0
 */
@Component
public class TimeByDayElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {

    private final String format = "yyyy-MM-dd";

    public TimeByDayElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super("time-by-day", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        return wrapIndex(index).concat("_").concat(DateFormatter.toString(new Date(), format));
    }
}
