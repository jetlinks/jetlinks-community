package org.jetlinks.community.things.data.operations;

import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.things.data.ThingsDataConstants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public abstract class AbstractDDLOperations implements DDLOperations {

    protected final String thingType;
    protected final String templateId;
    protected final String thingId;

    protected final DataSettings settings;

    protected final MetricBuilder metricBuilder;


    protected List<PropertyMetadata> createBasicColumns() {
        return Arrays
            .asList(
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_ID, "ID", StringType.GLOBAL),
                SimplePropertyMetadata.of(metricBuilder.getThingIdProperty(), "物ID", StringType.GLOBAL),
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_MESSAGE_ID, "消息ID", StringType.GLOBAL),
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_CREATE_TIME, "创建时间", DateTimeType.GLOBAL),
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_TIMESTAMP, "数据时间", DateTimeType.GLOBAL)
            );
    }

    protected abstract List<PropertyMetadata> createPropertyProperties(List<PropertyMetadata> propertyMetadata);

    protected List<PropertyMetadata> createLogProperties() {
        List<PropertyMetadata> metadata = new ArrayList<>(createBasicColumns());

        {
            metadata.add(SimplePropertyMetadata.of(
                ThingsDataConstants.COLUMN_LOG_TYPE,
                "日志类型",
                StringType.GLOBAL
            ));
        }

        {
            metadata.add(SimplePropertyMetadata.of(
                ThingsDataConstants.COLUMN_LOG_CONTENT,
                "日志内容",
                new StringType().expand(ConfigMetadataConstants.maxLength, 4096L)
            ));
        }

        return metadata;
    }

    protected List<PropertyMetadata> createEventProperties(EventMetadata event) {
        List<PropertyMetadata> metadata = new ArrayList<>(
            createBasicColumns()
        );

        DataType type = event.getType();
        if (type instanceof ObjectType) {
            if (CollectionUtils.isNotEmpty(((ObjectType) type).getProperties())) {
                metadata.addAll(((ObjectType) type).getProperties());
            }
        } else {
            metadata.add(
                SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_EVENT_VALUE, "事件数据", type)
            );
        }

        return metadata;
    }

    protected List<PropertyMetadata> createEventProperties() {
        List<PropertyMetadata> metadata = new ArrayList<>(
            createBasicColumns()
        );

        metadata.add(
            SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_EVENT_VALUE, "事件数据", StringType.GLOBAL)
        );

        return metadata;
    }

    private Mono<Void> doWith(ThingMetadata metadata,
                              Function3<MetricType, String, List<PropertyMetadata>, Mono<Void>> handler) {

        Mono<Void> properties = handler.apply(
            MetricType.properties,
            metricBuilder.createPropertyMetric(thingType, templateId, thingId),
            createPropertyProperties(metadata.getProperties()));

        Mono<Void> log = handler.apply(
            MetricType.log,
            metricBuilder.createLogMetric(thingType, templateId, thingId),
            createLogProperties());
        Mono<Void> event;
        if (settings.getEvent().eventIsAllInOne()) {
            event = handler.apply(MetricType.event,
                                  metricBuilder.createEventAllInOneMetric(thingType, templateId, thingId),
                                  createEventProperties());
        } else {
            event = Flux
                .fromIterable(metadata.getEvents())
                .flatMap(e -> handler.apply(MetricType.event,
                                            metricBuilder.createEventMetric(thingType, templateId, thingId, e.getId()),
                                            createEventProperties(e)))
                .then();
        }

        return Flux
            .concat(properties, log, event)
            .then();
    }

    @Override
    public final Mono<Void> registerMetadata(ThingMetadata metadata) {
        return doWith(metadata, this::register);
    }

    @Override
    public final Mono<Void> reloadMetadata(ThingMetadata metadata) {
        return doWith(metadata, this::reload);
    }

    protected abstract Mono<Void> register(MetricType metricType, String metric, List<PropertyMetadata> properties);

    protected abstract Mono<Void> reload(MetricType metricType, String metric, List<PropertyMetadata> properties);

    public enum MetricType {
        properties,
        log,
        event
    }
}
