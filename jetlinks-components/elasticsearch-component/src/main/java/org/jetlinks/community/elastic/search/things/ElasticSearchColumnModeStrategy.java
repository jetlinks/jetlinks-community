package org.jetlinks.community.elastic.search.things;

import lombok.AllArgsConstructor;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.*;

@AllArgsConstructor
public class ElasticSearchColumnModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final ElasticSearchService searchService;
    private final AggregationService aggregationService;
    private final ElasticSearchIndexManager indexManager;

    @Override
    public String getId() {
        return "default-column";
    }

    @Override
    public String getName() {
        return "ElasticSearch-列式存储";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new ElasticSearchColumnModeSaveOperations(
            registry,
            context.getMetricBuilder(),
            context.getSettings(),
            searchService);
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new ElasticSearchColumnModeQueryOperations(
            thingType,
            templateId,
            thingId,
            context.getMetricBuilder(),
            context.getSettings(),
            registry,
            searchService,
            aggregationService);
    }

    @Override
    protected DDLOperations createForDDL(String thingType, String templateId, String thingId, OperationsContext context) {
        return new ElasticSearchColumnModeDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            context.getMetricBuilder(),
            indexManager);
    }

    @Override
    public int getOrder() {
        return 10001;
    }
}
