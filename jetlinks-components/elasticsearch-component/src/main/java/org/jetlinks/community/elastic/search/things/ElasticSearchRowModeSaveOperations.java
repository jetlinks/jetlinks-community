package org.jetlinks.community.elastic.search.things;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeSaveOperationsBase;
import org.jetlinks.community.timeseries.TimeSeriesData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ElasticSearchRowModeSaveOperations extends RowModeSaveOperationsBase {

    private final ElasticSearchService searchService;

    public ElasticSearchRowModeSaveOperations(ThingsRegistry registry,
                                              MetricBuilder metricBuilder,
                                              DataSettings settings,
                                              ElasticSearchService searchService) {
        super(registry, metricBuilder, settings);
        this.searchService = searchService;
    }

    @Override
    protected Mono<Void> doSave(String metric, TimeSeriesData data) {
        return searchService.commit(metric, data.getData());
    }

    @Override
    protected Mono<Void> doSave(String metric, Flux<TimeSeriesData> data) {
        return searchService.save(metric, data.map(TimeSeriesData::getData));
    }
}
