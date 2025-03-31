package org.jetlinks.community.things.data.operations;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingMetadata;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public abstract class ColumnModeDDLOperationsBase extends AbstractDDLOperations{

    public ColumnModeDDLOperationsBase(String thingType,
                                       String templateId,
                                       String thingId,
                                       DataSettings settings,
                                       MetricBuilder metricBuilder) {
        super(thingType, templateId, thingId, settings, metricBuilder);
    }

    @Override
    protected List<PropertyMetadata> createPropertyProperties(List<PropertyMetadata> propertyMetadata) {
        List<PropertyMetadata> props = new ArrayList<>(createBasicColumns());
        props.addAll(propertyMetadata);
        return props;
    }

    @Override
    public Mono<Void> validateMetadata(ThingMetadata metadata) {
        return Mono.empty();
    }
}
