package org.jetlinks.community.elastic.search.index.strategies;

import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * 按每年第n周来划分索引策略
 *
 * @author zhouhao
 * @since 2.3
 */
public class TimeByWeekElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private static final WeekFields FIELDS = WeekFields.of(Locale.getDefault());

    public TimeByWeekElasticSearchIndexStrategy(ReactiveElasticsearchClient client,
                                                ElasticSearchIndexProperties properties) {
        super("time-by-week", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        LocalDate now = LocalDate.now(CLOCK);
        String idx = wrapIndex(index);

        return idx + "_" + now.getYear() + "-woy-" + now.get(FIELDS.weekOfYear());
    }
}
