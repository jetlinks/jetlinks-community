package org.jetlinks.community.elastic.search.manager;

import lombok.Getter;
import org.jetlinks.community.elastic.search.enums.IndexPatternEnum;
import org.jetlinks.community.elastic.search.enums.IndexStrategyEnum;
import org.jetlinks.community.elastic.search.manager.entity.IndexStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Getter
public class DefaultIndexStrategyProvider implements IndexStrategyProvider {


    @Value("${elasticsearch.time-series.index-strategy.suffix-month:}")
    private List<String> suffixMonthIndices;

    @Value("${elasticsearch.time-series.index-strategy.suffix-day:}")
    private List<String> suffixDayIndices;


    @Value("${elasticsearch.time-series.index-strategy.format:yyyy-MM}")
    private String format;

    @Value("${elasticsearch.time-series.index-strategy.connector:-}")
    private String connector;

    @Override
    public Map<String, IndexStrategy> getIndexStrategies() {
        Map<String, IndexStrategy> indexStrategyMap = new HashMap<>();
        suffixMonthIndices
            .stream()
            .filter(StringUtils::hasText)
            .forEach(s -> indexStrategyMap.put(s, new IndexStrategy(IndexStrategyEnum.SUFFIX.getValue(), IndexPatternEnum.MONTH.getValue())));
        suffixMonthIndices
            .stream()
            .filter(StringUtils::hasText)
            .forEach(s -> indexStrategyMap.put(s, new IndexStrategy(IndexStrategyEnum.SUFFIX.getValue(), IndexPatternEnum.DAY.getValue())));
        return indexStrategyMap;
    }
}
