package org.jetlinks.community.elastic.search.timeseries;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.jetlinks.community.elastic.search.index.IndexTemplateProvider;
import org.jetlinks.community.elastic.search.manager.StandardsIndexManager;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.jetlinks.community.elastic.search.service.IndexTemplateOperationService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Slf4j
public abstract class ESAbstractTimeSeriesManager implements TimeSeriesManager {


    private Map<String, LocalTimeSeriesMetric> localMetricMap = new ConcurrentHashMap<>();

    private StandardsIndexManager standardsIndexManager;

    private IndexOperationService indexOperationService;

    private IndexTemplateOperationService indexTemplateOperationService;

    // TODO: 2020/2/11 策略变更动态更新实现
    protected LocalTimeSeriesMetric getLocalTimeSeriesMetric(TimeSeriesMetric metric) {
        return localMetricMap.computeIfAbsent(metric.getId(), index -> {
            String standardsIndex = getStandardsIndexManager().getStandardsIndex(index);
            String templateName = IndexTemplateProvider.getIndexTemplate(index);
            return new LocalTimeSeriesMetric(
                getStandardsIndexManager().indexIsChange(index),
                index,
                standardsIndex,
                IndexAliasProvider.getIndexAlias(index),
                templateName,
                Collections.singletonList(index.concat("*")));
        });
    }

    @Getter
    @AllArgsConstructor
    static class LocalTimeSeriesMetric {

        private boolean indexHasStrategy;

        private String index;

        private String standardsIndex;

        private Alias alias;

        private String templateName;

        private List<String> indexTemplatePatterns;
    }
}