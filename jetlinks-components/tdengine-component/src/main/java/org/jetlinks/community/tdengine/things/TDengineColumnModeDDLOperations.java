package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.things.data.operations.ColumnModeDDLOperationsBase;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import reactor.core.publisher.Mono;

import java.util.List;

class TDengineColumnModeDDLOperations extends ColumnModeDDLOperationsBase {

    private final TDengineThingDataHelper helper;

    public TDengineColumnModeDDLOperations(String thingType,
                                           String templateId,
                                           String thingId,
                                           DataSettings settings,
                                           MetricBuilder metricBuilder,
                                           TDengineThingDataHelper helper) {
        super(thingType, templateId, thingId, settings, metricBuilder);
        this.helper = helper;
    }


    @Override
    protected Mono<Void> register(MetricType metricType,String metric, List<PropertyMetadata> properties) {
        helper.metadataManager.register(metric, properties);
        return Mono.empty();
    }

    @Override
    protected Mono<Void> reload(MetricType metricType,String metric, List<PropertyMetadata> properties) {
        helper.metadataManager.register(metric, properties);
        return Mono.empty();
    }
}
