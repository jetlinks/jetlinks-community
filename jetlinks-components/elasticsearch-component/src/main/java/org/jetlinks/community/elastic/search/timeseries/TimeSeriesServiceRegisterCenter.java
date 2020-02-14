package org.jetlinks.community.elastic.search.timeseries;

import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class TimeSeriesServiceRegisterCenter {

    private final ElasticSearchService elasticSearchService;

    private final AggregationService aggregationService;

    private Map<String, TimeSeriesService> serviceMap = new ConcurrentHashMap<>(16);

    public TimeSeriesServiceRegisterCenter(AggregationService aggregationService, ElasticSearchService elasticSearchService) {
        this.aggregationService = aggregationService;
        this.elasticSearchService = elasticSearchService;
    }

    public TimeSeriesService getTimeSeriesService(ESAbstractTimeSeriesManager.LocalTimeSeriesMetric metric) {
        return serviceMap.computeIfAbsent(metric.getIndex(), i -> new ESTimeSeriesService(metric, elasticSearchService, aggregationService));
    }
}
