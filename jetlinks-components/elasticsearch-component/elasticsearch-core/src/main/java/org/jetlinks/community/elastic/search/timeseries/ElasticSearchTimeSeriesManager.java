/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.elastic.search.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cache.Caches;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Slf4j
public class ElasticSearchTimeSeriesManager implements TimeSeriesManager {


    private final Map<String, TimeSeriesService> serviceMap = Caches.newCache();

    protected final ElasticSearchIndexManager indexManager;

    private final ElasticSearchService elasticSearchService;

    private final AggregationService aggregationService;


    public ElasticSearchTimeSeriesManager(ElasticSearchIndexManager indexManager,
                                          ElasticSearchService elasticSearchService,
                                          AggregationService aggregationService) {
        this.elasticSearchService = elasticSearchService;
        this.indexManager = indexManager;
        this.aggregationService = aggregationService;
    }

    @Override
    public TimeSeriesService getService(TimeSeriesMetric metric) {
        return getService(metric.getId());
    }

    @Override
    public TimeSeriesService getServices(TimeSeriesMetric... metric) {
        return getServices(Arrays
            .stream(metric)
            .map(TimeSeriesMetric::getId).toArray(String[]::new));
    }

    @Override
    public TimeSeriesService getServices(String... metric) {
        return new ElasticSearchTimeSeriesService(metric, elasticSearchService, aggregationService);
    }

    @Override
    public TimeSeriesService getService(String metric) {
        return serviceMap.computeIfAbsent(metric,
            id -> new ElasticSearchTimeSeriesService(new String[]{id}, elasticSearchService, aggregationService));
    }


    @Override
    public Mono<Void> registerMetadata(TimeSeriesMetadata metadata) {
        //默认字段
        SimplePropertyMetadata timestamp = new SimplePropertyMetadata();
        timestamp.setId("timestamp");
        timestamp.setValueType(new DateTimeType());
        return indexManager
            .putIndex(new DefaultElasticSearchIndexMetadata(metadata.getMetric().getId(), metadata.getProperties())
            .addProperty(timestamp));
    }

}
