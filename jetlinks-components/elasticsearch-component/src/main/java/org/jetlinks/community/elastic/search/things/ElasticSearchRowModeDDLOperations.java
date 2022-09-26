package org.jetlinks.community.elastic.search.things;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeDDLOperationsBase;
import reactor.core.publisher.Mono;

import java.util.List;

class ElasticSearchRowModeDDLOperations extends RowModeDDLOperationsBase {

    private final ElasticSearchIndexManager indexManager;

    public ElasticSearchRowModeDDLOperations(String thingType,
                                             String templateId,
                                             String thingId,
                                             DataSettings settings,
                                             MetricBuilder metricBuilder,
                                             ElasticSearchIndexManager indexManager) {
        super(thingType, templateId, thingId, settings, metricBuilder);
        this.indexManager = indexManager;
    }

    @Override
    protected Mono<Void> register(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        return indexManager
            .putIndex(new DefaultElasticSearchIndexMetadata(metric, properties));
    }

    @Override
    protected Mono<Void> reload(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        return indexManager
            .putIndex(new DefaultElasticSearchIndexMetadata(metric, properties));
    }
}
